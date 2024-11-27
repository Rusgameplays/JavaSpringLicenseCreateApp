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
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.DeviceLicense;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.DeviceLicenseRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.DeviceService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.ticket.Ticket;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/licenses")
public class LicenseInfoController {

    private final DeviceService deviceService;
    private final LicenseService licenseService;
    private final DeviceLicenseRepository deviceLicenseRepository;

    //TODO: Пользователь получит валидный тикет, если лицензия истекла?
    @GetMapping("/info")
    public ResponseEntity<?> getLicenseInfo(
            @RequestParam String mac) {
        try {
            Device device = deviceService.getByMac(mac);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Устройство не найдено");
            }
            //TODO: для одного устройства может быть больше одной лицензии
            DeviceLicense deviceLicense = deviceLicenseRepository.findByDeviceId(device.getId());
            if (deviceLicense == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Лицензия для устройства не найдена");
            }

            License license = deviceLicense.getLicense();
            if (license == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Лицензия не найдена");
            }

            Ticket ticket = licenseService.generateTicket(license, device);

            return ResponseEntity.ok(ticket);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }


}
