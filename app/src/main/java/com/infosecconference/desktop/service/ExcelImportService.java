package com.infosecconference.desktop.service;

import com.infosecconference.desktop.config.AppConfiguration;
import com.infosecconference.desktop.config.BrandingTheme;
import com.infosecconference.desktop.db.DatabaseManager;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Imports conference reference data from the Excel workbooks provided in the sessions.
 */
public class ExcelImportService {
    private static final String COUNTRIES_FILE = "Cтраны_import.xlsx";
    private static final String CITIES_FILE = "Город_import.xlsx";
    private static final String EVENTS_FILE = "Мероприятия_import/Мероприятия_Информационная безопасность.xlsx";
    private static final String ACTIVITIES_FILE = "Активности_import.xlsx";
    private static final String PARTICIPANTS_FILE = "Участники_import/участники-4.xlsx";
    private static final String MODERATORS_FILE = "Модераторы_import/Модераторы.xlsx";
    private static final String ORGANIZERS_FILE = "Организаторы_import/организаторы.xlsx";
    private static final String JURY_FILE = "Жюри_import/жюри-4.xlsx";

    private static final UserSheetLayout PARTICIPANT_LAYOUT =
            new UserSheetLayout(0, 1, 2, 3, 4, 5, null, null, 6, 7);
    private static final UserSheetLayout ORGANIZER_LAYOUT =
            new UserSheetLayout(0, 1, 2, 3, 4, 5, null, null, 6, 7);
    private static final UserSheetLayout MODERATOR_LAYOUT =
            new UserSheetLayout(0, 2, 3, 4, 5, 8, 6, 7, 9, 1);
    private static final UserSheetLayout JURY_LAYOUT =
            new UserSheetLayout(0, 2, 3, 4, 5, 7, 6, null, 8, 1);

    private final AppConfiguration configuration;
    private final DatabaseManager databaseManager;
    private final BrandingTheme theme;

    public ExcelImportService(AppConfiguration configuration,
                              DatabaseManager databaseManager,
                              BrandingTheme theme) {
        this.configuration = configuration;
        this.databaseManager = databaseManager;
        this.theme = theme;
    }

    public ImportReport importAll() throws IOException, SQLException {
        ImportReport report = new ImportReport();
        databaseManager.executeInTransaction(connection -> {
            Map<Integer, Long> countryIndex = importCountries(connection, report);
            Map<Integer, Long> cityIndex = importCities(connection, report, countryIndex);

            Map<String, Long> organizers = importUsers(connection, report, ORGANIZERS_FILE, "ORGANIZER", countryIndex, ORGANIZER_LAYOUT);
            Map<String, Long> moderators = importUsers(connection, report, MODERATORS_FILE, "MODERATOR", countryIndex, MODERATOR_LAYOUT);
            Map<String, Long> jury = importUsers(connection, report, JURY_FILE, "JURY", countryIndex, JURY_LAYOUT);
            Map<String, Long> participants = importUsers(connection, report, PARTICIPANTS_FILE, "PARTICIPANT", countryIndex, PARTICIPANT_LAYOUT);

            importEventsAndActivities(connection, report, cityIndex, organizers, moderators, jury, participants);
            return null;
        });
        return report;
    }

    private Map<Integer, Long> importCountries(Connection connection, ImportReport report) throws IOException, SQLException {
        Map<Integer, Long> countries = new HashMap<>();
        try (Workbook workbook = loadWorkbook(configuration.excelRoot().resolve(COUNTRIES_FILE))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean headerSkipped = false;
            int index = 1;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String name = formatter.formatCellValue(row.getCell(0)).trim();
                if (name.isEmpty()) {
                    continue;
                }
                String iso = formatter.formatCellValue(row.getCell(2)).trim();
                long id = upsertCountry(connection, name, iso.isEmpty() ? null : iso);
                countries.put(index, id);
                index++;
            }
            report.addCountries(countries.size());
        }
        return countries;
    }

    private Map<Integer, Long> importCities(Connection connection,
                                            ImportReport report,
                                            Map<Integer, Long> countriesByIndex) throws IOException, SQLException {
        Map<Integer, Long> cities = new HashMap<>();
        try (Workbook workbook = loadWorkbook(configuration.excelRoot().resolve(CITIES_FILE))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                String idValue = formatter.formatCellValue(row.getCell(0)).trim();
                String cityName = formatter.formatCellValue(row.getCell(2)).trim();
                if (cityName.isEmpty()) {
                    continue;
                }
                int index = parseIntSafe(idValue, cities.size() + 1);
                Long countryId = countriesByIndex.get(index);
                long cityId = upsertCity(connection, cityName, countryId);
                cities.put(index, cityId);
            }
            report.addCities(cities.size());
        }
        return cities;
    }

    private Map<String, Long> importUsers(Connection connection,
                                          ImportReport report,
                                          String fileName,
                                          String role,
                                          Map<Integer, Long> countriesByIndex,
                                          UserSheetLayout layout) throws IOException, SQLException {
        Path file = configuration.excelRoot().resolve(fileName);
        Map<String, Long> users = new HashMap<>();
        try (Workbook workbook = loadWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean headerSkipped = false;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String fullName = formatter.formatCellValue(row.getCell(layout.fullNameIndex())).trim();
                if (fullName.isEmpty()) {
                    continue;
                }
                String email = formatter.formatCellValue(row.getCell(layout.emailIndex())).trim();
                if (email.isEmpty()) {
                    email = generateEmail(fullName);
                }
                String birthValue = formatter.formatCellValue(row.getCell(layout.birthDateIndex())).trim();
                String countryRef = formatter.formatCellValue(row.getCell(layout.countryIndex())).trim();
                String phone = formatter.formatCellValue(row.getCell(layout.phoneIndex())).trim();
                String password = formatter.formatCellValue(row.getCell(layout.passwordIndex())).trim();
                String photo = formatter.formatCellValue(row.getCell(layout.photoIndex())).trim();
                String specialization = layout.specializationIndex() != null
                        ? formatter.formatCellValue(row.getCell(layout.specializationIndex())).trim() : null;
                String eventTitle = layout.eventIndex() != null
                        ? formatter.formatCellValue(row.getCell(layout.eventIndex())).trim() : null;
                String gender = layout.genderIndex() != null
                        ? formatter.formatCellValue(row.getCell(layout.genderIndex())).trim() : null;

                Long countryId = parseCountryReference(countryRef, countriesByIndex);
                Long cityId = null; // city is derived later via manual mapping
                LocalDate birthDate = parseExcelDate(birthValue);

                String organization = resolveOrganization(role, specialization, eventTitle);
                String bio = buildBio(role, specialization, gender, password);
                String photoPath = buildPhotoPath(role, photo);

                long userId = upsertUser(connection, fullName, email, role, birthDate, cityId, organization, phone, bio, photoPath, countryId);
                users.put(normalizeName(fullName), userId);
            }
        }

        switch (role) {
            case "ORGANIZER" -> report.addOrganizers(users.size());
            case "MODERATOR" -> report.addModerators(users.size());
            case "JURY" -> report.addJury(users.size());
            case "PARTICIPANT" -> report.addParticipants(users.size());
            default -> {
            }
        }
        return users;
    }

    private void importEventsAndActivities(Connection connection,
                                           ImportReport report,
                                           Map<Integer, Long> citiesByIndex,
                                           Map<String, Long> organizers,
                                           Map<String, Long> moderators,
                                           Map<String, Long> jury,
                                           Map<String, Long> participants) throws IOException, SQLException {
        Map<String, Long> eventsByTitle = new HashMap<>();
        try (Workbook workbook = loadWorkbook(configuration.excelRoot().resolve(EVENTS_FILE))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean headerSkipped = false;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String title = formatter.formatCellValue(row.getCell(1)).trim();
                if (title.isEmpty()) {
                    continue;
                }
                String startValue = formatter.formatCellValue(row.getCell(2)).trim();
                String durationValue = formatter.formatCellValue(row.getCell(3)).trim();
                String cityRef = formatter.formatCellValue(row.getCell(4)).trim();

                LocalDate startDate = parseExcelDate(startValue);
                long days = parseLongSafe(durationValue, 1);
                LocalDate endDate = startDate != null ? startDate.plusDays(Math.max(0, days - 1)) : null;
                Long cityId = parseCityReference(cityRef, citiesByIndex);

                String imagePath = "events/" + normalizeFileName(title) + ".jpg";
                long eventId = upsertEvent(connection, title, startDate, endDate, cityId, imagePath);
                eventsByTitle.put(title.toLowerCase(Locale.ROOT), eventId);
                upsertEventOrganizers(connection, eventId, organizers);
            }
            report.addEvents(eventsByTitle.size());
        }

        try (Workbook workbook = loadWorkbook(configuration.excelRoot().resolve(ACTIVITIES_FILE))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean headerSkipped = false;
            Long currentEventId = null;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String eventTitle = formatter.formatCellValue(row.getCell(1)).trim();
                if (!eventTitle.isEmpty()) {
                    currentEventId = eventsByTitle.get(eventTitle.toLowerCase(Locale.ROOT));
                }
                String activityTitle = formatter.formatCellValue(row.getCell(4)).trim();
                if (activityTitle.isEmpty()) {
                    continue;
                }
                long activityId = upsertActivity(connection, currentEventId, activityTitle);
                String dayValue = formatter.formatCellValue(row.getCell(5)).trim();
                String startValue = formatter.formatCellValue(row.getCell(6)).trim();
                LocalDateTime start = buildDateTime(connection, currentEventId, dayValue, startValue);
                LocalDateTime end = start != null ? start.plusHours(1) : null;
                updateActivitySchedule(connection, activityId, start, end);

                String moderatorName = formatter.formatCellValue(row.getCell(7)).trim();
                Long moderatorId = moderators.get(normalizeName(moderatorName));
                if (moderatorId != null) {
                    assignModerator(connection, activityId, moderatorId);
                }

                List<Long> juryMembers = new ArrayList<>();
                for (int i = 8; i <= 12; i++) {
                    String juryName = formatter.formatCellValue(row.getCell(i)).trim();
                    if (!juryName.isEmpty()) {
                        Long juryId = jury.get(normalizeName(juryName));
                        if (juryId != null) {
                            juryMembers.add(juryId);
                        }
                    }
                }
                assignJury(connection, activityId, juryMembers);

                String winnerName = formatter.formatCellValue(row.getCell(13)).trim();
                if (!winnerName.isEmpty()) {
                    Long winnerId = participants.get(normalizeName(winnerName));
                    if (winnerId != null) {
                        createWinnerTeam(connection, activityId, winnerName, winnerId);
                        report.addTeams(1);
                    }
                }
                report.addActivities(1);
            }
        }
    }

    private Workbook loadWorkbook(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Файл не найден: " + path);
        }
        try (InputStream stream = Files.newInputStream(path)) {
            return WorkbookFactory.create(stream);
        }
    }

    private long upsertCountry(Connection connection, String name, String isoCode) throws SQLException {
        Long existingId = findId(connection, "SELECT id FROM countries WHERE LOWER(name) = LOWER(?)", name);
        if (existingId != null) {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE countries SET iso_code = ? WHERE id = ?")) {
                statement.setString(1, isoCode);
                statement.setLong(2, existingId);
                statement.executeUpdate();
            }
            return existingId;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO countries(name, iso_code) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, isoCode);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Не удалось создать страну " + name);
    }

    private long upsertCity(Connection connection, String name, Long countryId) throws SQLException {
        Long existingId = findId(connection, "SELECT id FROM cities WHERE LOWER(name) = LOWER(?)", name);
        if (existingId != null) {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE cities SET country_id = ? WHERE id = ?")) {
                if (countryId != null) {
                    statement.setLong(1, countryId);
                } else {
                    statement.setNull(1, java.sql.Types.BIGINT);
                }
                statement.setLong(2, existingId);
                statement.executeUpdate();
            }
            return existingId;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO cities(name, country_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            if (countryId != null) {
                statement.setLong(2, countryId);
            } else {
                statement.setNull(2, java.sql.Types.BIGINT);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Не удалось создать город " + name);
    }

    private long upsertUser(Connection connection,
                             String fullName,
                             String email,
                             String role,
                             LocalDate birthDate,
                             Long cityId,
                             String organization,
                             String phone,
                             String bio,
                             String photoPath,
                             Long countryId) throws SQLException {
        Long existingId = findId(connection, "SELECT id FROM conference_users WHERE LOWER(email) = LOWER(?)", email);
        if (existingId != null) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE conference_users SET full_name = ?, role = ?, birth_date = ?, city_id = ?, organization = ?, phone = ?, bio = ?, photo_path = ?, country_id = ? WHERE id = ?")) {
                statement.setString(1, fullName);
                statement.setString(2, role);
                setDate(statement, 3, birthDate);
                setLong(statement, 4, cityId);
                statement.setString(5, organization);
                statement.setString(6, phone);
                statement.setString(7, bio);
                statement.setString(8, photoPath);
                setLong(statement, 9, countryId);
                statement.setLong(10, existingId);
                statement.executeUpdate();
            }
            return existingId;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO conference_users(full_name, email, role, birth_date, city_id, organization, phone, bio, photo_path, country_id) VALUES (?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, role);
            setDate(statement, 4, birthDate);
            setLong(statement, 5, cityId);
            statement.setString(6, organization);
            statement.setString(7, phone);
            statement.setString(8, bio);
            statement.setString(9, photoPath);
            setLong(statement, 10, countryId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Не удалось создать пользователя " + fullName);
    }

    private long upsertEvent(Connection connection,
                              String title,
                              LocalDate startDate,
                              LocalDate endDate,
                              Long cityId,
                              String imagePath) throws SQLException {
        Long existingId = findId(connection, "SELECT id FROM events WHERE LOWER(title) = LOWER(?)", title);
        if (existingId != null) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE events SET start_date = ?, end_date = ?, city_id = ?, venue = ?, image_path = ?, brand_color = ? WHERE id = ?")) {
                setDate(statement, 1, startDate);
                setDate(statement, 2, endDate);
                setLong(statement, 3, cityId);
                statement.setString(4, cityId != null ? "Главная площадка" : "Онлайн");
                statement.setString(5, imagePath);
                statement.setString(6, colorToHex(theme.primaryColor()));
                statement.setLong(7, existingId);
                statement.executeUpdate();
            }
            return existingId;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO events(title, description, start_date, end_date, city_id, venue, image_path, brand_color) VALUES (?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, "Программа конференции «" + title + "»." );
            setDate(statement, 3, startDate);
            setDate(statement, 4, endDate);
            setLong(statement, 5, cityId);
            statement.setString(6, cityId != null ? "Главная площадка" : "Онлайн");
            statement.setString(7, imagePath);
            statement.setString(8, colorToHex(theme.primaryColor()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Не удалось создать мероприятие " + title);
    }

    private void upsertEventOrganizers(Connection connection, long eventId, Map<String, Long> organizers) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM event_organizers WHERE event_id = ?")) {
            delete.setLong(1, eventId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO event_organizers(event_id, user_id) VALUES (?, ?)");) {
            int count = 0;
            for (Long organizerId : organizers.values()) {
                insert.setLong(1, eventId);
                insert.setLong(2, organizerId);
                insert.addBatch();
                count++;
                if (count >= 3) {
                    break;
                }
            }
            insert.executeBatch();
        }
    }

    private long upsertActivity(Connection connection, Long eventId, String title) throws SQLException {
        if (eventId == null) {
            throw new SQLException("Активность " + title + " не привязана к мероприятию. Убедитесь, что лист мероприятий импортирован.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM activities WHERE event_id = ? AND LOWER(name) = LOWER(?)")) {
            statement.setLong(1, eventId);
            statement.setString(2, title);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO activities(event_id, name, description) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setString(2, title);
            statement.setString(3, "Активность в рамках конференции");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Не удалось создать активность " + title);
    }

    private void updateActivitySchedule(Connection connection, long activityId, LocalDateTime start, LocalDateTime end) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE activities SET start_time = ?, end_time = ? WHERE id = ?")) {
            setTimestamp(statement, 1, start);
            setTimestamp(statement, 2, end);
            statement.setLong(3, activityId);
            statement.executeUpdate();
        }
    }

    private void assignModerator(Connection connection, long activityId, long moderatorId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE activities SET moderator_id = ? WHERE id = ?")) {
            statement.setLong(1, moderatorId);
            statement.setLong(2, activityId);
            statement.executeUpdate();
        }
    }

    private void assignJury(Connection connection, long activityId, List<Long> juryMembers) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM activity_jury WHERE activity_id = ?")) {
            delete.setLong(1, activityId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO activity_jury(activity_id, user_id) VALUES (?, ?)");) {
            for (Long juryId : juryMembers) {
                insert.setLong(1, activityId);
                insert.setLong(2, juryId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void createWinnerTeam(Connection connection, long activityId, String teamName, long winnerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO teams(name, track, score) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, teamName + " — победители");
            statement.setString(2, "CyberShield Challenge");
            statement.setInt(3, 100);
            statement.executeUpdate();
            long teamId;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Не удалось получить идентификатор команды");
                }
                teamId = keys.getLong(1);
            }
            try (PreparedStatement link = connection.prepareStatement(
                    "INSERT INTO team_participants(team_id, user_id, activity_id) VALUES (?,?,?)")) {
                link.setLong(1, teamId);
                link.setLong(2, winnerId);
                link.setLong(3, activityId);
                link.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE activities SET winner_team = ? WHERE id = ?")) {
                update.setString(1, teamName);
                update.setLong(2, activityId);
                update.executeUpdate();
            }
        }
    }

    private Long findId(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private void setDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        if (date != null) {
            statement.setDate(index, Date.valueOf(date));
        } else {
            statement.setNull(index, java.sql.Types.DATE);
        }
    }

    private void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value != null) {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        } else {
            statement.setNull(index, java.sql.Types.TIMESTAMP);
        }
    }

    private void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value != null) {
            statement.setLong(index, value);
        } else {
            statement.setNull(index, java.sql.Types.BIGINT);
        }
    }

    private LocalDate parseExcelDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double numeric = Double.parseDouble(value);
            return DateUtil.getLocalDateTime(numeric).toLocalDate();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime buildDateTime(Connection connection, Long eventId, String dayValue, String timeValue) throws SQLException {
        if (eventId == null) {
            return null;
        }
        LocalDate startDate = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT start_date FROM events WHERE id = ?")) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next() && rs.getDate(1) != null) {
                    startDate = rs.getDate(1).toLocalDate();
                }
            }
        }
        if (startDate == null) {
            return null;
        }
        long dayOffset = parseLongSafe(dayValue, 1) - 1;
        LocalDate date = startDate.plusDays(Math.max(0, dayOffset));
        LocalTime time = parseExcelTime(timeValue);
        return LocalDateTime.of(date, time != null ? time : LocalTime.of(9, 0));
    }

    private LocalTime parseExcelTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double numeric = Double.parseDouble(value);
            return DateUtil.getLocalDateTime(numeric).toLocalTime();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return (int) Math.round(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private long parseLongSafe(String value, long defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Math.round(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String generateEmail(String fullName) {
        return normalizeName(fullName).replace(' ', '.') + "@cybershield.example";
    }

    private String resolveOrganization(String role, String specialization, String eventTitle) {
        return switch (role) {
            case "ORGANIZER" -> "Организационный комитет";
            case "MODERATOR" -> eventTitle != null && !eventTitle.isBlank() ? eventTitle : "Модератор активностей";
            case "JURY" -> "Судейская коллегия";
            case "PARTICIPANT" -> "Участник программы";
            default -> specialization;
        };
    }

    private String buildBio(String role, String specialization, String gender, String password) {
        StringBuilder bio = new StringBuilder();
        if (specialization != null && !specialization.isBlank()) {
            bio.append("Направление: ").append(specialization);
        }
        if (gender != null && !gender.isBlank()) {
            if (!bio.isEmpty()) {
                bio.append(". ");
            }
            bio.append("Пол: ").append(gender);
        }
        if ("PARTICIPANT".equals(role) && password != null && !password.isBlank()) {
            if (!bio.isEmpty()) {
                bio.append(". ");
            }
            bio.append("Временный пароль: ").append(password);
        }
        return bio.isEmpty() ? null : bio.toString();
    }

    private String buildPhotoPath(String role, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String folder = switch (role) {
            case "MODERATOR" -> "moderators";
            case "ORGANIZER" -> "organizers";
            case "JURY" -> "jury";
            case "PARTICIPANT" -> "participants";
            default -> "users";
        };
        return folder + "/" + fileName;
    }

    private Long parseCountryReference(String value, Map<Integer, Long> countries) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return countries.get(parseIntSafe(value, -1));
    }

    private Long parseCityReference(String value, Map<Integer, Long> cities) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return cities.get(parseIntSafe(value, -1));
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFileName(String value) {
        return value == null ? "event" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private String colorToHex(java.awt.Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
