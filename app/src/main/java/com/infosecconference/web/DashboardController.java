package com.infosecconference.web;

import com.infosecconference.model.Activity;
import com.infosecconference.model.Event;
import com.infosecconference.model.UserRole;
import com.infosecconference.service.ActivityService;
import com.infosecconference.service.ConferenceUserService;
import com.infosecconference.service.EventService;
import com.infosecconference.service.ExcelImportService;
import com.infosecconference.service.ImportReport;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {
    private final EventService eventService;
    private final ActivityService activityService;
    private final ConferenceUserService userService;
    private final ExcelImportService importService;

    public DashboardController(EventService eventService,
                               ActivityService activityService,
                               ConferenceUserService userService,
                               ExcelImportService importService) {
        this.eventService = eventService;
        this.activityService = activityService;
        this.userService = userService;
        this.importService = importService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Event> events = eventService.findAll().stream()
                .sorted(Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        List<Activity> activities = activityService.findAll().stream()
                .sorted(Comparator.comparing(Activity::getStartDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .toList();

        model.addAttribute("events", events);
        model.addAttribute("activities", activities);
        model.addAttribute("participantsCount", userService.findByRole(UserRole.PARTICIPANT).size());
        model.addAttribute("moderatorsCount", userService.findByRole(UserRole.MODERATOR).size());
        model.addAttribute("juryCount", userService.findByRole(UserRole.JURY).size());
        model.addAttribute("organizersCount", userService.findByRole(UserRole.ORGANIZER).size());
        return "index";
    }

    @PostMapping("/import")
    public String importData(RedirectAttributes redirectAttributes) {
        ImportReport report = importService.importAll();
        redirectAttributes.addFlashAttribute("importReport", report);
        return "redirect:/";
    }
}
