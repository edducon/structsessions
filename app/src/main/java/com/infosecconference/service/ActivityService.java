package com.infosecconference.service;

import com.infosecconference.model.Activity;
import com.infosecconference.model.Event;
import com.infosecconference.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ActivityService {
    private final ActivityRepository repository;

    public ActivityService(ActivityRepository repository) {
        this.repository = repository;
    }

    public List<Activity> findAll() {
        return repository.findAll();
    }

    public List<Activity> findByEvent(Event event) {
        return repository.findByEvent(event);
    }

    public Optional<Activity> findById(Long id) {
        return repository.findById(id);
    }

    public Activity save(Activity activity) {
        return repository.save(activity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
