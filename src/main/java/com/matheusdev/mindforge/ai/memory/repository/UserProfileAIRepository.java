package com.matheusdev.mindforge.ai.memory.repository;

import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileAIRepository extends JpaRepository<UserProfileAI, Long> {
}
