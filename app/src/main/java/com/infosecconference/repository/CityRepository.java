package com.infosecconference.repository;

import com.infosecconference.model.City;
import com.infosecconference.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByNameIgnoreCaseAndCountry(String name, Country country);
    Optional<City> findByNameIgnoreCase(String name);
}
