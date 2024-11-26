package ru.mtuci.demo.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.configuration.JwtTokenProvider;
import ru.mtuci.demo.controller.requests.LicenseRequest;
import ru.mtuci.demo.controller.requests.UpdateLicenseRequest;
import ru.mtuci.demo.exception.UserNull;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.services.DeviceService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.services.UserService;
import ru.mtuci.demo.ticket.Ticket;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/licenses")
@RestController
@PreAuthorize("hasAnyRole('ADMIN')")
public class LicenseController {
    private final LicenseService licenseService;
    private final DeviceService deviceService;

    @GetMapping
    public List<License> getAll() {
        return licenseService.getAll();
    }

    @GetMapping("/{id}")
    public License getById(@PathVariable("id") Long id) {
        return licenseService.getById(id);
    }

    @GetMapping("/key/{key}")
    public License getByKey(@PathVariable("key") String key) {
        return licenseService.getByKey(key);
    }

    @PostMapping("/add")
    public ResponseEntity<String> add(@RequestBody LicenseRequest licenseRequest) {
        try {

            License createdLicense = licenseService.createLicense(licenseRequest.getProductId(),
                    licenseRequest.getOwnerId(),
                    licenseRequest.getLicenseTypeId());


            return ResponseEntity.ok("Лицензия успешно создана с ID: " + createdLicense.getId());
        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        } catch (UserNull e) {

            return ResponseEntity.badRequest().body("Пользователь не найден");
        } catch (Exception e) {

            return ResponseEntity.status(500).body("Ошибка при создании лицензии: " + e.getMessage());
        }
    }

    @PostMapping("/renew")
    public ResponseEntity<?> renewLicense(@RequestBody UpdateLicenseRequest updateLicenseRequest) {
        try {
            Device device = deviceService.getByMac(updateLicenseRequest.getDeviceMac());
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Устройство не найдено по указанному MAC адресу");
            }

            User user = device.getUser();

            License oldLicense = licenseService.getByUser(user);

            License newLicense = licenseService.getByKey(updateLicenseRequest.getLicenseKey());

            if (newLicense == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Лицензия не найдена по указанному ключу");
            }

            if (newLicense.getActivationDate() != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Лицензия уже активирована");
            }

            Integer defaultDuration = newLicense.getLicenseType().getDefaultDuration();


            LocalDate currentExpirationDate = oldLicense.getExpirationDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();


            LocalDate newExpirationDate = currentExpirationDate.plusMonths(defaultDuration);

            Date newExpiration = Date.from(newExpirationDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            oldLicense.setExpirationDate(newExpiration);

            oldLicense.setActivationDate(new Date());

            newLicense.setUser(oldLicense.getUser());
            newLicense.setBlocked(oldLicense.getBlocked());

            licenseService.delete(oldLicense);

            newLicense.setExpirationDate(newExpiration);
            newLicense.setActivationDate(new Date());
            licenseService.add(newLicense);

            Ticket ticket = licenseService.generateTicket(newLicense, device);

            return ResponseEntity.ok(ticket);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + ex.getMessage());
        }
    }

}




