package com.wikicook.auth.repository;

import com.wikicook.auth.domain.OneTimeToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {
}
