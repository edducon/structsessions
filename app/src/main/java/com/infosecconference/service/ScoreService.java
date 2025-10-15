package com.infosecconference.service;

import com.infosecconference.model.Activity;
import com.infosecconference.model.ConferenceUser;
import com.infosecconference.model.Score;
import com.infosecconference.repository.ScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class ScoreService {
    private final ScoreRepository repository;

    public ScoreService(ScoreRepository repository) {
        this.repository = repository;
    }

    public List<Score> findAll() {
        return repository.findAll();
    }

    public List<Score> findByActivity(Activity activity) {
        return repository.findByActivity(activity);
    }

    public List<Score> findByParticipant(ConferenceUser participant) {
        return repository.findByParticipant(participant);
    }

    public Score submitScore(Score score) {
        return repository.save(score);
    }

    public BigDecimal calculateAverageScoreForParticipant(Activity activity, ConferenceUser participant) {
        var scores = repository.findByParticipant(participant).stream()
                .filter(score -> score.getActivity().equals(activity))
                .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = scores.stream()
                .map(Score::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
