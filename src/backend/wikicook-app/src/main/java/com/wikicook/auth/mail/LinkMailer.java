package com.wikicook.auth.mail;

import com.wikicook.auth.config.AuthProperties;
import com.wikicook.auth.mail.MailRenderer.Rendered;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends the emails that carry a one-time link (research R8): verification now, password reset in a later phase. The raw
 * token is never stored, so these cannot go through the outbox; instead the mail goes out <em>after the transaction
 * commits</em> (never for a rolled-back one), on a virtual thread so the response does not wait for SMTP, with three
 * attempts. When all fail it only logs: the account exists, and the user asks for a new link (Edge Cases, FR-017).
 */
@Component
public class LinkMailer {

    private static final Logger log = LoggerFactory.getLogger(LinkMailer.class);
    private static final int ATTEMPTS = 3;
    private static final int VERIFICATION_HOURS = 24;

    private final JavaMailSender sender;
    private final MailRenderer renderer;
    private final AuthProperties properties;
    private final Executor executor;
    private final Duration retryDelay;

    @Autowired
    public LinkMailer(JavaMailSender sender, MailRenderer renderer, AuthProperties properties) {
        this(sender, renderer, properties, Executors.newVirtualThreadPerTaskExecutor(), Duration.ofSeconds(1));
    }

    LinkMailer(JavaMailSender sender, MailRenderer renderer, AuthProperties properties, Executor executor,
            Duration retryDelay) {
        this.sender = sender;
        this.renderer = renderer;
        this.properties = properties;
        this.executor = executor;
        this.retryDelay = retryDelay;
    }

    /** Mails the verification link; {@code token} is the raw one-time token. */
    public void sendVerification(String toEmail, String displayName, String token) {
        Rendered mail = renderer.render("verify-email", Map.of("name", displayName,
                "link", verificationLink(properties.publicBaseUrl(), token), "hours",
                Integer.toString(VERIFICATION_HOURS)));
        afterCommit(() -> executor.execute(() -> deliver(toEmail, mail)));
    }

    /** The token goes in the fragment: it never reaches a proxy log or a Referer header, and a link scanner that only
     * GETs the page does not use it (research R6). */
    static String verificationLink(String publicBaseUrl, String token) {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return base + "/verify-email#token=" + token;
    }

    private static void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void deliver(String toEmail, Rendered mail) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                sender.send(build(toEmail, mail));
                return;
            } catch (Exception e) {
                if (attempt == ATTEMPTS) {
                    log.error("Giving up on a verification email after {} attempts: {}", ATTEMPTS, e.toString());
                    return;
                }
                log.warn("Verification email attempt {} failed: {}", attempt, e.toString());
                pause();
            }
        }
    }

    private MimeMessage build(String toEmail, Rendered mail) throws Exception {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(properties.mailFrom());
        helper.setTo(toEmail);
        helper.setSubject(mail.subject());
        helper.setText(mail.text(), mail.html());
        return message;
    }

    private void pause() {
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
