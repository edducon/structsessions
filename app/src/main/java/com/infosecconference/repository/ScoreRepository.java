package com.infosecconference.repository;

import com.infosecconference.model.Activity;
import com.infosecconference.model.ConferenceUser;
import com.infosecconference.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByActivity(Activity activity);
    List<Score> findByParticipant(ConferenceUser participant);
}
