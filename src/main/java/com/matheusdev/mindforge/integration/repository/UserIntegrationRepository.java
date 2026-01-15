package com.matheusdev.mindforge.integration.repository;

import com.matheusdev.mindforge.integration.model.UserIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserIntegrationRepository extends JpaRepository<UserIntegration, Long> {
    Optional<UserIntegration> findByUserIdAndProvider(Long userId, UserIntegration.Provider provider);
}
