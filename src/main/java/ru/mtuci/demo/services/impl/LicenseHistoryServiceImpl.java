package ru.mtuci.demo.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.LicenseHistory;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.LicenseHistoryRepository;
import ru.mtuci.demo.services.LicenseHistoryService;

import java.util.Date;

//TODO: хотелось бы ещё и получать все события в хронологическом порядке
@Service
public class LicenseHistoryServiceImpl implements LicenseHistoryService {

    @Autowired
    private LicenseHistoryRepository licenseHistoryRepository;

    @Override
    public void recordLicenseChange(License license, User user, String status, String description) {
        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus(status);
        history.setDescription(description);
        history.setChangeDate(new Date());

        licenseHistoryRepository.save(history);
    }
}
