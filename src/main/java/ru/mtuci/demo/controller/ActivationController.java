package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.controller.requests.LicenseActivationRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.DeviceLicenseService;
import ru.mtuci.demo.services.DeviceService;
import ru.mtuci.demo.services.LicenseHistoryService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.ticket.Ticket;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ActivationController {

    private final LicenseService licenseService;
    private final DeviceService deviceService;
    private final LicenseHistoryService licenseHistoryService;
    private final UserRepository userRepository;
    private final DeviceLicenseService deviceLicenseService;


    @PostMapping("/activate")
    public ResponseEntity<?> activateLicense(@RequestBody LicenseActivationRequest request) {
        try {
            User authenticatedUser = getAuthenticatedUser();

            User user = userRepository.findById(request.getDeviceRequest().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            License license = licenseService.getByKey(request.getKey());
            if (license == null) {
                return ResponseEntity.badRequest().body("Лицензия не найдена");
            }

            if (license.getActivationDate() != null) {
                return ResponseEntity.badRequest().body("Лицензия уже была активирована");
            }

            Device device = deviceService.registerOrUpdateDevice(request.getDeviceRequest());

            boolean activationValid = licenseService.validateActivation(license, device, authenticatedUser);
            if (!activationValid) {
                return ResponseEntity.badRequest().body("Активация невозможна");
            }

            long activeDeviceCount = licenseService.countActiveDevicesForUser(user);
            if (activeDeviceCount >= license.getMaxDevices()) {
                return ResponseEntity.badRequest().body("Превышено максимальное количество устройств для данной лицензии");
            }

            deviceLicenseService.addDeviceToLicense(license, device);

            license.setUser(user);
            licenseService.updateLicense(license, authenticatedUser);

            licenseHistoryService.recordLicenseChange(license, authenticatedUser, "Activated", "Лицензия успешно активирована");

            Ticket fullTicket = licenseService.generateTicket(license, device);

            Ticket responseTicket = new Ticket();
            responseTicket.setServerDate(fullTicket.getServerDate());
            responseTicket.setTicketLifetime(fullTicket.getTicketLifetime());
            responseTicket.setActivationDate(fullTicket.getActivationDate());
            responseTicket.setExpirationDate(fullTicket.getExpirationDate());
            responseTicket.setUserId(fullTicket.getUserId());
            responseTicket.setDeviceId(fullTicket.getDeviceId());
            responseTicket.setLicenseBlocked(fullTicket.getLicenseBlocked());
            responseTicket.setDigitalSignature(fullTicket.getDigitalSignature());

            return ResponseEntity.ok(responseTicket);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }






    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = (String) authentication.getPrincipal();
            return userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        return null;
    }




}
