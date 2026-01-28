package com.matheusdev.mindforge.core.auth.repository;

import com.matheusdev.mindforge.core.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
