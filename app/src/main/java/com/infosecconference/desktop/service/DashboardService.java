package com.infosecconference.desktop.service;

import com.infosecconference.desktop.db.DatabaseManager;
import com.infosecconference.desktop.model.Activity;
import com.infosecconference.desktop.model.City;
import com.infosecconference.desktop.model.ConferenceUser;
import com.infosecconference.desktop.model.Country;
import com.infosecconference.desktop.model.Event;
import com.infosecconference.desktop.model.SummarySnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardService {
    private final DatabaseManager databaseManager;

    public DashboardService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public SummarySnapshot loadSummary() throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            long events = count(connection, "SELECT COUNT(*) FROM events");
            long activities = count(connection, "SELECT COUNT(*) FROM activities");
            long participants = count(connection, "SELECT COUNT(*) FROM conference_users WHERE role = 'PARTICIPANT'");
            long moderators = count(connection, "SELECT COUNT(*) FROM conference_users WHERE role = 'MODERATOR'");
            long jury = count(connection, "SELECT COUNT(*) FROM conference_users WHERE role = 'JURY'");
            long organizers = count(connection, "SELECT COUNT(*) FROM conference_users WHERE role = 'ORGANIZER'");
            long teams = count(connection, "SELECT COUNT(*) FROM teams");
            return new SummarySnapshot(events, activities, participants, moderators, jury, organizers, teams);
        }
    }

    public List<ConferenceUser> loadUsersByRole(String role) throws SQLException {
        String sql = """
                SELECT u.id, u.full_name, u.email, u.role, u.birth_date, u.organization, u.phone, u.photo_path,
                       c.id AS city_id, c.name AS city_name,
                       co.id AS country_id, co.name AS country_name, co.iso_code
                FROM conference_users u
                LEFT JOIN cities c ON u.city_id = c.id
                LEFT JOIN countries co ON c.country_id = co.id
                WHERE u.role = ?
                ORDER BY u.full_name
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role);
            try (ResultSet rs = statement.executeQuery()) {
                List<ConferenceUser> result = new ArrayList<>();
                while (rs.next()) {
                    Country country = null;
                    long countryId = rs.getLong("country_id");
                    if (!rs.wasNull()) {
                        country = new Country(countryId, rs.getString("country_name"), rs.getString("iso_code"));
                    }
                    City city = null;
                    long cityId = rs.getLong("city_id");
                    if (!rs.wasNull()) {
                        city = new City(cityId, rs.getString("city_name"), country);
                    }
                    result.add(new ConferenceUser(
                            rs.getLong("id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getDate("birth_date") != null ? rs.getDate("birth_date").toLocalDate() : null,
                            city,
                            rs.getString("organization"),
                            rs.getString("phone"),
                            rs.getString("photo_path")));
                }
                return result;
            }
        }
    }

    public List<Activity> loadUpcomingActivities(int limit) throws SQLException {
        String sql = """
                SELECT a.id, a.name, a.start_time, a.end_time, a.description, a.winner_team,
                       e.id AS event_id, e.title, e.description AS event_description, e.start_date, e.end_date, e.venue, e.image_path,
                       c.id AS city_id, c.name AS city_name,
                       co.id AS country_id, co.name AS country_name, co.iso_code,
                       m.id AS moderator_id, m.full_name, m.email, m.role AS moderator_role,
                       m.birth_date AS moderator_birth, m.organization AS moderator_org,
                       m.phone AS moderator_phone, m.photo_path AS moderator_photo,
                       mc.id AS moderator_city_id, mc.name AS moderator_city_name,
                       mco.id AS moderator_country_id, mco.name AS moderator_country_name, mco.iso_code AS moderator_country_iso
                FROM activities a
                JOIN events e ON a.event_id = e.id
                LEFT JOIN cities c ON e.city_id = c.id
                LEFT JOIN countries co ON c.country_id = co.id
                LEFT JOIN conference_users m ON a.moderator_id = m.id
                LEFT JOIN cities mc ON m.city_id = mc.id
                LEFT JOIN countries mco ON mc.country_id = mco.id
                WHERE a.start_time >= NOW()
                ORDER BY a.start_time
                LIMIT ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<Activity> activities = new ArrayList<>();
                while (rs.next()) {
                    Country eventCountry = null;
                    long eventCountryId = rs.getLong("country_id");
                    if (!rs.wasNull()) {
                        eventCountry = new Country(eventCountryId, rs.getString("country_name"), rs.getString("iso_code"));
                    }
                    City eventCity = null;
                    long eventCityId = rs.getLong("city_id");
                    if (!rs.wasNull()) {
                        eventCity = new City(eventCityId, rs.getString("city_name"), eventCountry);
                    }
                    Event event = new Event(
                            rs.getLong("event_id"),
                            rs.getString("title"),
                            rs.getString("event_description"),
                            rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                            rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null,
                            eventCity,
                            rs.getString("venue"),
                            rs.getString("image_path"));

                    ConferenceUser moderator = null;
                    long moderatorId = rs.getLong("moderator_id");
                    if (!rs.wasNull()) {
                        Country moderatorCountry = null;
                        long modCountryId = rs.getLong("moderator_country_id");
                        if (!rs.wasNull()) {
                            moderatorCountry = new Country(modCountryId,
                                    rs.getString("moderator_country_name"),
                                    rs.getString("moderator_country_iso"));
                        }
                        City moderatorCity = null;
                        long modCityId = rs.getLong("moderator_city_id");
                        if (!rs.wasNull()) {
                            moderatorCity = new City(modCityId,
                                    rs.getString("moderator_city_name"),
                                    moderatorCountry);
                        }
                        moderator = new ConferenceUser(
                                moderatorId,
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getString("moderator_role"),
                                rs.getDate("moderator_birth") != null ? rs.getDate("moderator_birth").toLocalDate() : null,
                                moderatorCity,
                                rs.getString("moderator_org"),
                                rs.getString("moderator_phone"),
                                rs.getString("moderator_photo"));
                    }

                    LocalDateTime start = rs.getTimestamp("start_time") != null
                            ? rs.getTimestamp("start_time").toLocalDateTime()
                            : null;
                    LocalDateTime end = rs.getTimestamp("end_time") != null
                            ? rs.getTimestamp("end_time").toLocalDateTime()
                            : null;

                    activities.add(new Activity(
                            rs.getLong("id"),
                            rs.getString("name"),
                            event,
                            start,
                            end,
                            moderator,
                            rs.getString("description"),
                            rs.getString("winner_team")));
                }
                return activities;
            }
        }
    }

    public List<Event> loadEvents() throws SQLException {
        String sql = """
                SELECT e.id, e.title, e.description, e.start_date, e.end_date, e.venue, e.image_path,
                       c.id AS city_id, c.name AS city_name,
                       co.id AS country_id, co.name AS country_name, co.iso_code
                FROM events e
                LEFT JOIN cities c ON e.city_id = c.id
                LEFT JOIN countries co ON c.country_id = co.id
                ORDER BY e.start_date
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Event> events = new ArrayList<>();
            while (rs.next()) {
                Country country = null;
                long countryId = rs.getLong("country_id");
                if (!rs.wasNull()) {
                    country = new Country(countryId, rs.getString("country_name"), rs.getString("iso_code"));
                }
                City city = null;
                long cityId = rs.getLong("city_id");
                if (!rs.wasNull()) {
                    city = new City(cityId, rs.getString("city_name"), country);
                }
                events.add(new Event(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                        rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null,
                        city,
                        rs.getString("venue"),
                        rs.getString("image_path")));
            }
            return events;
        }
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        }
    }
}
