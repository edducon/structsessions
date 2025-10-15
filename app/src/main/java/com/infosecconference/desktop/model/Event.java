package com.infosecconference.desktop.model;

import java.time.LocalDate;

public record Event(long id,
                    String title,
                    String description,
                    LocalDate startDate,
                    LocalDate endDate,
                    City city,
                    String venue,
                    String imagePath) {
}
