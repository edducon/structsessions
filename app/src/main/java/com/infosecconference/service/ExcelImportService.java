package com.infosecconference.service;

import com.infosecconference.model.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class ExcelImportService {
    private static final String COUNTRIES_FILE = "db/import/Cтраны_import.xlsx";
    private static final String CITIES_FILE = "db/import/Город_import.xlsx";
    private static final String EVENTS_FILE = "db/import/Мероприятия_import/Мероприятия_Информационная безопасность.xlsx";
    private static final String ACTIVITIES_FILE = "db/import/Активности_import.xlsx";
    private static final String PARTICIPANTS_FILE = "db/import/Участники_import/участники-4.xlsx";
    private static final String MODERATORS_FILE = "db/import/Модераторы_import/Модераторы.xlsx";
    private static final String ORGANIZERS_FILE = "db/import/Организаторы_import/организаторы.xlsx";
    private static final String JURY_FILE = "db/import/Жюри_import/жюри-4.xlsx";

    private static final UserSheetLayout PARTICIPANT_LAYOUT =
            new UserSheetLayout(1, 2, 3, 4, 5, 6, null, null, 7);
    private static final UserSheetLayout ORGANIZER_LAYOUT =
            new UserSheetLayout(1, 2, 3, 4, 5, 6, null, null, 7);
    private static final UserSheetLayout MODERATOR_LAYOUT =
            new UserSheetLayout(2, 3, 4, 5, 8, 9, 6, 7, 1);
    private static final UserSheetLayout JURY_LAYOUT =
            new UserSheetLayout(2, 3, 4, 5, 7, 8, 6, null, 1);

    private final ResourceLoader resourceLoader;
    private final CountryService countryService;
    private final CityService cityService;
    private final ConferenceUserService userService;
    private final EventService eventService;
    private final ActivityService activityService;
    private final TeamService teamService;

    public ExcelImportService(ResourceLoader resourceLoader,
                              CountryService countryService,
                              CityService cityService,
                              ConferenceUserService userService,
                              EventService eventService,
                              ActivityService activityService,
                              TeamService teamService) {
        this.resourceLoader = resourceLoader;
        this.countryService = countryService;
        this.cityService = cityService;
        this.userService = userService;
        this.eventService = eventService;
        this.activityService = activityService;
        this.teamService = teamService;
    }

    public ImportReport importAll() {
        ImportReport report = new ImportReport();
        Map<Integer, Country> countriesByIndex = importCountries(report);
        Map<Integer, City> citiesByIndex = importCities(report, countriesByIndex);

        Map<String, ConferenceUser> organizers = importUsers(report, ORGANIZERS_FILE, UserRole.ORGANIZER, countriesByIndex, ORGANIZER_LAYOUT);
        Map<String, ConferenceUser> moderators = importUsers(report, MODERATORS_FILE, UserRole.MODERATOR, countriesByIndex, MODERATOR_LAYOUT);
        Map<String, ConferenceUser> jury = importUsers(report, JURY_FILE, UserRole.JURY, countriesByIndex, JURY_LAYOUT);
        Map<String, ConferenceUser> participants = importUsers(report, PARTICIPANTS_FILE, UserRole.PARTICIPANT, countriesByIndex, PARTICIPANT_LAYOUT);

        importEventsAndActivities(report, citiesByIndex, organizers, moderators, jury, participants);

        return report;
    }

    private Map<Integer, Country> importCountries(ImportReport report) {
        Map<Integer, Country> countries = new HashMap<>();
        try (Workbook workbook = loadWorkbook(COUNTRIES_FILE)) {
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
                Country country = countryService.findOrCreate(name, iso.isEmpty() ? null : iso);
                countries.put(index, country);
                index++;
            }
            report.addCountries(countries.size());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить список стран", e);
        }
        return countries;
    }

    private Map<Integer, City> importCities(ImportReport report, Map<Integer, Country> countriesByIndex) {
        Map<Integer, City> cities = new HashMap<>();
        try (Workbook workbook = loadWorkbook(CITIES_FILE)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                String idValue = formatter.formatCellValue(row.getCell(0));
                String cityName = formatter.formatCellValue(row.getCell(2)).trim();
                if (cityName.isEmpty()) {
                    continue;
                }
                int id = parseIntSafe(idValue, cities.size() + 1);
                Country country = countriesByIndex.get(id);
                City city = cityService.findOrCreate(cityName, country);
                cities.put(id, city);
            }
            report.addCities(cities.size());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить список городов", e);
        }
        return cities;
    }

    private Map<String, ConferenceUser> importUsers(ImportReport report,
                                                     String fileName,
                                                     UserRole role,
                                                     Map<Integer, Country> countriesByIndex,
                                                     UserSheetLayout layout) {
        Map<String, ConferenceUser> users = new HashMap<>();
        try (Workbook workbook = loadWorkbook(fileName)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean headerSkipped = false;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String fullName = formatter.formatCellValue(row.getCell(0)).trim();
                if (fullName.isEmpty()) {
                    continue;
                }
                String email = formatter.formatCellValue(row.getCell(layout.emailIndex())).trim();
                if (email.isEmpty()) {
                    email = generateEmail(fullName);
                }
                String birthDateValue = formatter.formatCellValue(row.getCell(layout.birthDateIndex())).trim();
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

                Country country = parseCountryReference(countryRef, countriesByIndex);
                ConferenceUser user = userService.findByEmail(email)
                        .orElseGet(() -> new ConferenceUser(extractFirstName(fullName), extractLastName(fullName), email, role));
                user.setRole(role);
                user.setPhone(phone);
                user.setOrganization(resolveOrganization(role, specialization, eventTitle));
                user.setBio(buildBio(role, specialization, gender, password));
                user.setCountry(country);
                user.setPhotoPath(buildPhotoPath(role, photo));
                user.setDateOfBirth(parseExcelDate(birthDateValue));
                user.setCity(null);

                ConferenceUser saved = userService.save(user);
                users.put(normalizeName(fullName), saved);
            }
            report.addUsers(users.size());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить пользователей из " + fileName, e);
        }
        return users;
    }

    private void importEventsAndActivities(ImportReport report,
                                           Map<Integer, City> citiesByIndex,
                                           Map<String, ConferenceUser> organizers,
                                           Map<String, ConferenceUser> moderators,
                                           Map<String, ConferenceUser> jury,
                                           Map<String, ConferenceUser> participants) {
        Map<String, Event> eventsByTitle = new HashMap<>();
        try (Workbook eventsWorkbook = loadWorkbook(EVENTS_FILE)) {
            Sheet sheet = eventsWorkbook.getSheetAt(0);
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
                String startDateValue = formatter.formatCellValue(row.getCell(2)).trim();
                String daysValue = formatter.formatCellValue(row.getCell(3)).trim();
                String cityRef = formatter.formatCellValue(row.getCell(4)).trim();
                LocalDate startDate = parseExcelDate(startDateValue);
                long duration = parseLongSafe(daysValue, 1);
                Event event = eventService.findByTitle(title)
                        .orElseGet(() -> {
                            Event created = new Event(title);
                            created.setDescription("Программа конференции «" + title + "».");
                            return created;
                        });
                event.setStartDate(startDate);
                event.setEndDate(startDate != null ? startDate.plusDays(Math.max(0, duration - 1)) : null);
                City city = citiesByIndex.get(parseIntSafe(cityRef, -1));
                event.setCity(city);
                event.setVenueName(city != null ? "Главная площадка " + city.getName() : "Онлайн");
                event.setLogoPath("images/events/" + normalizeFileName(title) + ".jpg");
                event.getOrganizers().clear();
                event.getOrganizers().addAll(selectOrganizers(organizers));
                eventsByTitle.put(title.toLowerCase(Locale.ROOT), eventService.save(event));
            }
            report.addEvents(eventsByTitle.size());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить мероприятия", e);
        }

        try (Workbook activitiesWorkbook = loadWorkbook(ACTIVITIES_FILE)) {
            Sheet sheet = activitiesWorkbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean headerSkipped = false;
            Event currentEvent = null;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String eventName = formatter.formatCellValue(row.getCell(1)).trim();
                if (!eventName.isEmpty()) {
                    currentEvent = eventsByTitle.get(eventName.toLowerCase(Locale.ROOT));
                }
                String activityTitle = formatter.formatCellValue(row.getCell(4)).trim();
                if (activityTitle.isEmpty()) {
                    continue;
                }
                Activity activity = findOrCreateActivity(currentEvent, activityTitle);
                activity.setDescription("Активность в рамках мероприятия " + (currentEvent != null ? currentEvent.getTitle() : ""));
                activity.setEvent(currentEvent);
                String dayValue = formatter.formatCellValue(row.getCell(5)).trim();
                String startTimeValue = formatter.formatCellValue(row.getCell(6)).trim();
                activity.setStartDateTime(buildDateTime(currentEvent, dayValue, startTimeValue));
                activity.setEndDateTime(activity.getStartDateTime() != null ? activity.getStartDateTime().plusHours(1) : null);
                activity.setLocation(currentEvent != null && currentEvent.getCity() != null
                        ? currentEvent.getCity().getName() + ", Главный зал"
                        : "Онлайн");
                activity.setModerator(moderators.get(normalizeName(formatter.formatCellValue(row.getCell(7)).trim())));

                Set<ConferenceUser> activityJury = new HashSet<>();
                for (int i = 8; i <= 12; i++) {
                    String juryName = formatter.formatCellValue(row.getCell(i)).trim();
                    if (!juryName.isEmpty()) {
                        ConferenceUser juryMember = jury.get(normalizeName(juryName));
                        if (juryMember != null) {
                            activityJury.add(juryMember);
                        }
                    }
                }
                activity.setJuryMembers(activityJury);

                Activity savedActivity = activityService.save(activity);
                report.addActivities(1);

                String winnerName = formatter.formatCellValue(row.getCell(13)).trim();
                if (!winnerName.isEmpty()) {
                    ConferenceUser winner = participants.get(normalizeName(winnerName));
                    if (winner != null) {
                        Team team = new Team(winnerName + " — победители");
                        team.setActivity(savedActivity);
                        team.getParticipants().add(winner);
                        winner.getTeams().add(team);
                        teamService.save(team);
                        report.addTeams(1);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить активности", e);
        }
    }

    private Workbook loadWorkbook(String classpathLocation) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + classpathLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Файл " + classpathLocation + " отсутствует. Скопируйте ресурсы перед импортом.");
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return WorkbookFactory.create(inputStream);
        }
    }

    private Country parseCountryReference(String value, Map<Integer, Country> countriesByIndex) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int index = parseIntSafe(value, -1);
        return countriesByIndex.get(index);
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

    private LocalDateTime buildDateTime(Event event, String dayValue, String timeValue) {
        if (event == null || event.getStartDate() == null) {
            return null;
        }
        long dayOffset = parseLongSafe(dayValue, 1) - 1;
        LocalDate date = event.getStartDate().plusDays(Math.max(0, dayOffset));
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

    private String buildPhotoPath(UserRole role, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String folder = switch (role) {
            case MODERATOR -> "images/moderators";
            case ORGANIZER -> "images/organizers";
            case JURY -> "images/jury";
            case PARTICIPANT -> "images/participants";
            default -> "images/users";
        };
        return folder + "/" + fileName;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFileName(String value) {
        return value == null ? "event" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");
    }

    private String extractFirstName(String fullName) {
        String[] parts = fullName.split(" ");
        return parts.length > 1 ? parts[1] : parts[0];
    }

    private String extractLastName(String fullName) {
        String[] parts = fullName.split(" ");
        return parts[0];
    }

    private String generateEmail(String fullName) {
        return normalizeName(fullName).replace(' ', '.') + "@cybershield.example";
    }

    private String resolveOrganization(UserRole role, String specialization, String eventTitle) {
        return switch (role) {
            case ORGANIZER -> "Организационный комитет";
            case MODERATOR -> eventTitle != null && !eventTitle.isBlank() ? eventTitle : "Модератор активностей";
            case JURY -> "Судейская коллегия";
            case PARTICIPANT -> "Участник программы";
            default -> specialization;
        };
    }

    private String buildBio(UserRole role, String specialization, String gender, String password) {
        StringBuilder bio = new StringBuilder();
        if (specialization != null && !specialization.isBlank()) {
            bio.append("Направление: ").append(specialization);
        }
        if (gender != null && !gender.isBlank()) {
            if (bio.length() > 0) {
                bio.append(". ");
            }
            bio.append("Пол: ").append(gender);
        }
        if (role == UserRole.PARTICIPANT && password != null && !password.isBlank()) {
            if (bio.length() > 0) {
                bio.append(". ");
            }
            bio.append("Временный пароль: ").append(password);
        }
        return bio.length() == 0 ? null : bio.toString();
    }

    private Collection<ConferenceUser> selectOrganizers(Map<String, ConferenceUser> organizers) {
        return organizers.values().stream().limit(3).toList();
    }

    private Activity findOrCreateActivity(Event event, String activityTitle) {
        if (event == null) {
            return new Activity(activityTitle);
        }
        return activityService.findByEvent(event).stream()
                .filter(existing -> existing.getTitle().equalsIgnoreCase(activityTitle))
                .findFirst()
                .orElseGet(() -> new Activity(activityTitle));
    }
}
