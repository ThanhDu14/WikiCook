package com.wikicook.auth.repository;

import com.wikicook.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code users} as spec 010 sees it. Queries are added by the services that need them. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
}
