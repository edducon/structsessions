package com.infosecconference.service;

import com.infosecconference.model.Country;
import com.infosecconference.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CountryService {
    private final CountryRepository countryRepository;

    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public List<Country> findAll() {
        return countryRepository.findAll();
    }

    public Country findOrCreate(String name, String isoCode) {
        return countryRepository.findByNameIgnoreCase(name)
                .map(existing -> {
                    existing.setIsoCode(isoCode);
                    return countryRepository.save(existing);
                })
                .orElseGet(() -> countryRepository.save(new Country(name, isoCode)));
    }
}
