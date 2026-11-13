package com.wikicook.auth.repository;

import com.wikicook.auth.domain.BlockedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedEmailRepository extends JpaRepository<BlockedEmail, String> {
}
