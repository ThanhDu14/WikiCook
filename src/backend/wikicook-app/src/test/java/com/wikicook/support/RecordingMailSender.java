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
        private final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();

        public List<MimeMessage> messages() {
            return List.copyOf(messages);
        }

        /** How many times a send was tried, failed attempts included. */
        public int attempts() {
            return attempts.get();
        }

        /** The text of a message: its text/plain and text/html parts, in that order. */
        public static java.util.List<String> parts(MimeMessage message) {
            java.util.List<String> parts = new java.util.ArrayList<>();
            try {
                collect(message.getContent(), parts);
            } catch (Exception e) {
                throw new IllegalStateException("cannot read the message", e);
            }
            return parts;
        }

        private static void collect(Object content, java.util.List<String> parts) throws Exception {
            if (content instanceof String text) {
                parts.add(text);
            } else if (content instanceof jakarta.mail.Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    collect(multipart.getBodyPart(i).getContent(), parts);
                }
            }
        }

        /** The messages sent to this address. */
        public java.util.List<MimeMessage> messagesTo(String address) {
            return messages.stream().filter(m -> {
                try {
                    return m.getRecipients(jakarta.mail.Message.RecipientType.TO) != null
                            && java.util.Arrays.stream(m.getRecipients(jakarta.mail.Message.RecipientType.TO))
                                    .anyMatch(a -> a.toString().equalsIgnoreCase(address));
                } catch (jakarta.mail.MessagingException e) {
                    return false;
                }
            }).toList();
        }

        public void clear() {
            messages.clear();
            failures = 0;
            attempts.set(0);
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
            attempts.incrementAndGet();
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
