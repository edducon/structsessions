package com.infosecconference.desktop.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportReport {
    private int countries;
    private int cities;
    private int events;
    private int activities;
    private int participants;
    private int moderators;
    private int jury;
    private int organizers;
    private int teams;
    private final List<String> warnings = new ArrayList<>();

    public void addCountries(int amount) {
        countries += amount;
    }

    public void addCities(int amount) {
        cities += amount;
    }

    public void addEvents(int amount) {
        events += amount;
    }

    public void addActivities(int amount) {
        activities += amount;
    }

    public void addParticipants(int amount) {
        participants += amount;
    }

    public void addModerators(int amount) {
        moderators += amount;
    }

    public void addJury(int amount) {
        jury += amount;
    }

    public void addOrganizers(int amount) {
        organizers += amount;
    }

    public void addTeams(int amount) {
        teams += amount;
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public int countries() {
        return countries;
    }

    public int cities() {
        return cities;
    }

    public int events() {
        return events;
    }

    public int activities() {
        return activities;
    }

    public int participants() {
        return participants;
    }

    public int moderators() {
        return moderators;
    }

    public int jury() {
        return jury;
    }

    public int organizers() {
        return organizers;
    }

    public int teams() {
        return teams;
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
