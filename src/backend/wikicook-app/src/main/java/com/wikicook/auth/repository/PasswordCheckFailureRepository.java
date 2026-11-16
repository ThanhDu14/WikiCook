package com.wikicook.auth.repository;

import com.wikicook.auth.domain.PasswordCheckFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordCheckFailureRepository extends JpaRepository<PasswordCheckFailure, Long> {
}
