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
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;
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


    @GetMapping("/info")
    public ResponseEntity<?> getLicenseInfo(
            @RequestParam String deviceName,
            @RequestParam String mac) {
        try {
            Device device = deviceService.findDeviceByInfo(deviceName, mac);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Устройство не найдено");
            }

            User owner = device.getUser();
            if (owner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь, которому принадлежит устройство, не найден");
            }

            List<License> activeLicenses = licenseService.getActiveLicensesForUser(owner);
            if (activeLicenses.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Активные лицензии не найдены");
            }

            License firstLicense = activeLicenses.get(0);
            Ticket ticket = licenseService.generateTicket(firstLicense, device);

            return ResponseEntity.ok(ticket);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }

}
