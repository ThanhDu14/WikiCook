package com.wikicook.auth.repository;

import com.wikicook.auth.domain.UserSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {
}
