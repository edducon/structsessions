package com.infosecconference.repository;

import com.infosecconference.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByTitleIgnoreCase(String title);
}
