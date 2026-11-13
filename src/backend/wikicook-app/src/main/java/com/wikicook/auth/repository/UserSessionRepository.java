package com.wikicook.auth.repository;

import com.wikicook.auth.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, Long>, UserSessionRepositoryCustom {
}
