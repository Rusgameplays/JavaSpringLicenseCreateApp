package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.demo.controller.requests.LicenseHistoryResponse;
import ru.mtuci.demo.model.*;
import ru.mtuci.demo.repo.DeviceLicenseRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.DeviceService;
import ru.mtuci.demo.services.LicenseHistoryService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.ticket.Ticket;

import java.util.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/licenses")
public class LicenseInfoController {

    private final DeviceService deviceService;
    private final LicenseService licenseService;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseHistoryService licenseHistoryService;

    //TODO: Пользователь получит валидный тикет, если лицензия истекла? - Добавлена проверка
    @GetMapping("/info")
    public ResponseEntity<?> getLicenseInfo(@RequestParam String mac) {
        try {
            Device device = deviceService.getByMac(mac);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Устройство не найдено");
            }

            List<DeviceLicense> deviceLicenses = deviceLicenseRepository.findByDeviceId(device.getId());
            if (deviceLicenses.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Лицензии для устройства не найдены");
            }

            List<Ticket> tickets = new ArrayList<>();
            for (DeviceLicense deviceLicense : deviceLicenses) {
                License license = deviceLicense.getLicense();
                if (license == null) {
                    continue;
                }

                if (license.getExpirationDate() != null && license.getExpirationDate().before(new Date())) {
                    continue;
                }

                Ticket ticket = new Ticket(license,device);
                tickets.add(ticket);
            }

            if (tickets.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Все лицензии истекли");
            }

            return ResponseEntity.ok(tickets);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }

    //TODO: хотелось бы ещё и получать все события в хронологическом порядке - Исправил - выводит по возрастанию
    @GetMapping("/history")
    public ResponseEntity<?> getAllLicenseHistory() {
        try {
            List<LicenseHistoryResponse> historyList = licenseHistoryService.getAllLicenseHistory();
            if (historyList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("История лицензий не найдена");
            }
            return ResponseEntity.ok(historyList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }
}
