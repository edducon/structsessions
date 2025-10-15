package com.infosecconference.desktop.model;

public record SummarySnapshot(long events,
                              long activities,
                              long participants,
                              long moderators,
                              long jury,
                              long organizers,
                              long teams) {
}
