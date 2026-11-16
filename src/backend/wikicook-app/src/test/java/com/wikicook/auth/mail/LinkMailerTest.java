package com.wikicook.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.support.RecordingMailSender.Recorder;
import com.wikicook.support.TestAuthProperties;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * FR-005, research R8: the verification link is mailed after the transaction commits, off the request thread, and an
 * SMTP failure is retried three times and then only logged: the user asks for the email again (Edge Cases).
 */
class LinkMailerTest {

    private final Recorder smtp = new Recorder();
    private final LinkMailer mailer = new LinkMailer(smtp, new MailRenderer(), TestAuthProperties.create(),
            Runnable::run, Duration.ZERO);

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsOneMultipartMailWithTheLinkBuiltFromThePublicBaseUrl() throws Exception {
        mailer.sendVerification("an@mail.com", "An", "tok_en-123");

        assertThat(smtp.messages()).hasSize(1);
        MimeMessage message = smtp.messages().get(0);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0]).hasToString("an@mail.com");
        assertThat(message.getFrom()[0]).hasToString("no-reply@wikicook.local");
        assertThat(message.getSubject()).containsIgnoringCase("xác minh");
        List<String> parts = Recorder.parts(message);
        assertThat(parts).hasSize(2);
        assertThat(parts).allSatisfy(part -> assertThat(part)
                .contains("http://localhost:3000/verify-email#token=tok_en-123"));
        assertThat(parts.get(0)).doesNotContain("<html"); // text first
        assertThat(parts.get(1)).contains("<html");
    }

    @Test
    void theLinkPutsTheTokenInTheFragmentSoItNeverReachesALogOrAReferer() {
        assertThat(LinkMailer.verificationLink("https://wikicook.vn", "abc")).isEqualTo(
                "https://wikicook.vn/verify-email#token=abc");
        assertThat(LinkMailer.verificationLink("https://wikicook.vn/", "abc")).isEqualTo(
                "https://wikicook.vn/verify-email#token=abc");
    }

    @Test
    void retriesUpToThreeTimesWhenSmtpFails() {
        smtp.failNext(2);

        mailer.sendVerification("an@mail.com", "An", "tok");

        assertThat(smtp.attempts()).isEqualTo(3);
        assertThat(smtp.messages()).hasSize(1);
    }

    @Test
    void givesUpAfterThreeFailuresWithoutThrowing() {
        smtp.failNext(10);

        mailer.sendVerification("an@mail.com", "An", "tok"); // must not throw: the request already succeeded

        assertThat(smtp.attempts()).isEqualTo(3);
        assertThat(smtp.messages()).isEmpty();
    }

    @Test
    void insideATransactionTheMailGoesOutOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        mailer.sendVerification("an@mail.com", "An", "tok");

        assertThat(smtp.messages()).as("nothing before the commit").isEmpty();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(smtp.messages()).hasSize(1);
    }

    @Test
    void aRolledBackTransactionSendsNothing() {
        TransactionSynchronizationManager.initSynchronization();

        mailer.sendVerification("an@mail.com", "An", "tok");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(smtp.messages()).isEmpty();
    }

    @Test
    void theMailIsSentOnTheExecutorNotOnTheCallingThread() {
        List<Thread> threads = new java.util.ArrayList<>();
        LinkMailer async = new LinkMailer(smtp, new MailRenderer(), TestAuthProperties.create(), task -> {
            Thread thread = new Thread(task);
            threads.add(thread);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, Duration.ZERO);

        async.sendVerification("an@mail.com", "An", "tok");

        assertThat(threads).hasSize(1);
        assertThat(smtp.messages()).hasSize(1);
    }
}
