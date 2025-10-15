package com.infosecconference.service;

import com.infosecconference.model.Team;
import com.infosecconference.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TeamService {
    private final TeamRepository repository;

    public TeamService(TeamRepository repository) {
        this.repository = repository;
    }

    public List<Team> findAll() {
        return repository.findAll();
    }

    public Optional<Team> findById(Long id) {
        return repository.findById(id);
    }

    public Team save(Team team) {
        return repository.save(team);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
