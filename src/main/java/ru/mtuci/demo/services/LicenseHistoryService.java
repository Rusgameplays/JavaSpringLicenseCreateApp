package ru.mtuci.demo.services;

import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;

public interface LicenseHistoryService {
    void recordLicenseChange(License license, User user, String status, String description);
}
