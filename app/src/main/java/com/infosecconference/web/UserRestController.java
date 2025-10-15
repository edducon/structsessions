package com.infosecconference.web;

import com.infosecconference.model.ConferenceUser;
import com.infosecconference.model.UserRole;
import com.infosecconference.service.ConferenceUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {
    private final ConferenceUserService userService;

    public UserRestController(ConferenceUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<ConferenceUser> all() {
        return userService.findAll();
    }

    @GetMapping("/role/{role}")
    public List<ConferenceUser> byRole(@PathVariable UserRole role) {
        return userService.findByRole(role);
    }
}
