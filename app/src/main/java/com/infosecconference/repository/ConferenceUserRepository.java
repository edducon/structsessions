package com.infosecconference.repository;

import com.infosecconference.model.ConferenceUser;
import com.infosecconference.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConferenceUserRepository extends JpaRepository<ConferenceUser, Long> {
    Optional<ConferenceUser> findByEmail(String email);
    List<ConferenceUser> findByRole(UserRole role);
}
