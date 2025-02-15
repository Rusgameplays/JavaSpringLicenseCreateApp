package ru.mtuci.demo.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.controller.requests.LicenseHistoryResponse;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.LicenseHistory;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.LicenseHistoryRepository;
import ru.mtuci.demo.services.LicenseHistoryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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

    @Override
    public List<LicenseHistoryResponse> getAllLicenseHistory() {
        List<LicenseHistory> historyList = licenseHistoryRepository.findAllByOrderByChangeDateAsc(); //по возрастанию
        List<LicenseHistoryResponse> historyResponseList = new ArrayList<>();

        for (LicenseHistory history : historyList) {
            historyResponseList.add(new LicenseHistoryResponse(
                    history.getLicense() != null ? history.getLicense().getId() : null,
                    history.getUser() != null ? history.getUser().getId() : null,
                    history.getStatus(),
                    history.getChangeDate(),
                    history.getDescription()
            ));
        }

        return historyResponseList;
    }
}
