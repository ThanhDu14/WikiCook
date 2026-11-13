package com.wikicook.support;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * A {@code @Primary JavaMailSender} that records every message instead of talking to SMTP (research R14), so CI
 * needs no mail server. Import it with {@code @Import(RecordingMailSender.class)}. {@link #failNext(int)} makes the
 * next sends throw, for the outbox retry tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RecordingMailSender {

    private static final Recorder RECORDER = new Recorder();

    @Bean
    @Primary
    JavaMailSender recordingJavaMailSender() {
        return RECORDER;
    }

    public static Recorder recorder() {
        return RECORDER;
    }

    /** The sender itself: a static singleton so tests can read it without wiring. */
    public static class Recorder implements JavaMailSender {

        private final List<MimeMessage> messages = new CopyOnWriteArrayList<>();
        private volatile int failures;

        public List<MimeMessage> messages() {
            return List.copyOf(messages);
        }

        public void clear() {
            messages.clear();
            failures = 0;
        }

        public void failNext(int count) {
            this.failures = count;
        }

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getInstance(new Properties()));
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            try {
                return new MimeMessage(Session.getInstance(new Properties()), contentStream);
            } catch (jakarta.mail.MessagingException e) {
                throw new MailSendException("cannot parse message", e);
            }
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            if (failures > 0) {
                failures--;
                throw new MailSendException("recording sender: simulated SMTP failure");
            }
            messages.add(mimeMessage);
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            for (MimeMessage message : mimeMessages) {
                send(message);
            }
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            throw new UnsupportedOperationException("010 sends MIME messages only");
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            throw new UnsupportedOperationException("010 sends MIME messages only");
        }
    }
}
