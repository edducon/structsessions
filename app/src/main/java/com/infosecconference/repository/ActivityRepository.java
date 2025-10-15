package com.infosecconference.repository;

import com.infosecconference.model.Activity;
import com.infosecconference.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByEvent(Event event);
}
