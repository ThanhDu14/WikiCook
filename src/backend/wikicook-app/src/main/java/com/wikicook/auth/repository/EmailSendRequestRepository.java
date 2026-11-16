package com.wikicook.auth.repository;

import com.wikicook.auth.domain.EmailSendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSendRequestRepository extends JpaRepository<EmailSendRequest, Long> {
}
