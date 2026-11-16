package com.wikicook.auth.repository;

import com.wikicook.auth.domain.EmailOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOutboxRepository extends JpaRepository<EmailOutboxMessage, Long> {
}
