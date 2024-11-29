package ru.mtuci.demo.services;

import org.springframework.http.ResponseEntity;
import ru.mtuci.demo.controller.requests.LicenseHistoryResponse;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.LicenseHistory;
import ru.mtuci.demo.model.User;

import java.util.List;

public interface LicenseHistoryService {
    void recordLicenseChange(License license, User user, String status, String description);
    List<LicenseHistoryResponse> getAllLicenseHistory();
}
