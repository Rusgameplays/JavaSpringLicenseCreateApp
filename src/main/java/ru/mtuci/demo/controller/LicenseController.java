package ru.mtuci.demo.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.controller.requests.LicenseRequest;
import ru.mtuci.demo.exception.UserNull;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.services.LicenseService;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/licenses")
@RestController
@PreAuthorize("hasAnyRole('ADMIN')")
public class LicenseController {
    private final LicenseService licenseService;

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
}
