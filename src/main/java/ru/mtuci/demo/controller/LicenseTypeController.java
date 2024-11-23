package ru.mtuci.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.model.LicenseType;
import ru.mtuci.demo.services.LicenseTypeService;

import java.util.List;

@RestController
@RequestMapping("/license-types")
public class LicenseTypeController {

    private final LicenseTypeService licenseTypeService;

    public LicenseTypeController(LicenseTypeService licenseTypeService) {
        this.licenseTypeService = licenseTypeService;
    }

    @PreAuthorize("hasAnyRole('ADMIN')") // Ограничение доступа
    @PostMapping("/add")
    public ResponseEntity<String> addLicenseType(@RequestBody LicenseType licenseType) {
        try {
            LicenseType createdLicenseType = licenseTypeService.addLicenseType(licenseType);
            return ResponseEntity.ok("Тип лицензии успешно создан с ID: " + createdLicenseType.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при создании типа лицензии: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public ResponseEntity<LicenseType> getLicenseTypeById(@PathVariable Long id) {
        try {
            LicenseType licenseType = licenseTypeService.getLicenseTypeById(id);
            return ResponseEntity.ok(licenseType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<List<LicenseType>> getAllLicenseTypes() {
        return ResponseEntity.ok(licenseTypeService.getAllLicenseTypes());
    }
}
