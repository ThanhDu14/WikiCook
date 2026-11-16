package com.wikicook.auth.domain;

/** Notification emails that go through {@code email_outbox}; emails with a link are sent directly (research R8). */
public enum MailTemplate {
    ACCOUNT_LOCKED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_BANNED,
    SUSPENSION_ENDED,
    BAN_LIFTED
}
