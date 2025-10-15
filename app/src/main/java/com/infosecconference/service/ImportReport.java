package com.infosecconference.service;

public class ImportReport {
    private int countries;
    private int cities;
    private int users;
    private int events;
    private int activities;
    private int teams;

    public void addCountries(int count) {
        this.countries += count;
    }

    public void addCities(int count) {
        this.cities += count;
    }

    public void addUsers(int count) {
        this.users += count;
    }

    public void addEvents(int count) {
        this.events += count;
    }

    public void addActivities(int count) {
        this.activities += count;
    }

    public void addTeams(int count) {
        this.teams += count;
    }

    public int getCountries() {
        return countries;
    }

    public int getCities() {
        return cities;
    }

    public int getUsers() {
        return users;
    }

    public int getEvents() {
        return events;
    }

    public int getActivities() {
        return activities;
    }

    public int getTeams() {
        return teams;
    }
}
