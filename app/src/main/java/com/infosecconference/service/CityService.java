package com.infosecconference.service;

import com.infosecconference.model.City;
import com.infosecconference.model.Country;
import com.infosecconference.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CityService {
    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<City> findAll() {
        return cityRepository.findAll();
    }

    public City findOrCreate(String name, Country country) {
        if (country == null) {
            return cityRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> cityRepository.save(new City(name, null)));
        }
        return cityRepository.findByNameIgnoreCaseAndCountry(name, country)
                .orElseGet(() -> cityRepository.save(new City(name, country)));
    }
}
