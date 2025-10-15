package com.infosecconference.service;

import com.infosecconference.model.ConferenceUser;
import com.infosecconference.model.UserRole;
import com.infosecconference.repository.ConferenceUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ConferenceUserService {
    private final ConferenceUserRepository repository;

    public ConferenceUserService(ConferenceUserRepository repository) {
        this.repository = repository;
    }

    public List<ConferenceUser> findAll() {
        return repository.findAll();
    }

    public List<ConferenceUser> findByRole(UserRole role) {
        return repository.findByRole(role);
    }

    public Optional<ConferenceUser> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<ConferenceUser> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public ConferenceUser save(ConferenceUser user) {
        return repository.save(user);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
