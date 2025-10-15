package com.infosecconference.desktop.model;

import java.time.LocalDateTime;

public record Activity(long id,
                        String name,
                        Event event,
                        LocalDateTime startTime,
                        LocalDateTime endTime,
                        ConferenceUser moderator,
                        String description,
                        String winnerTeam) {
}
