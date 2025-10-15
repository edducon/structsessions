package com.infosecconference.desktop.model;

import java.time.LocalDate;

public record ConferenceUser(long id,
                              String fullName,
                              String email,
                              String role,
                              LocalDate birthDate,
                              City city,
                              String organization,
                              String phone,
                              String photoPath) {
}
