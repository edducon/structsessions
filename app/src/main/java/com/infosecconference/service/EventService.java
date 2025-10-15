package com.infosecconference.service;

import com.infosecconference.model.Event;
import com.infosecconference.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {
    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public List<Event> findAll() {
        return repository.findAll();
    }

    public Optional<Event> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Event> findByTitle(String title) {
        return repository.findByTitleIgnoreCase(title);
    }

    public Event save(Event event) {
        return repository.save(event);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
