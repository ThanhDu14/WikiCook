package com.wikicook.aiplanner.controller;

import org.springframework.http.HttpStatus;

/** Even the non-AI draft failed, e.g. the database is down (US6-6). The cause is logged, never returned. */
public class BackupUnavailableException extends AiPlannerException {

    public BackupUnavailableException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "BACKUP_UNAVAILABLE", "No draft could be made right now", cause);
    }
}
