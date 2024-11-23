package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.exception.ProductNull;
import ru.mtuci.demo.exception.TypeofLicenseNull;
import ru.mtuci.demo.exception.UserNull;
import ru.mtuci.demo.model.*;
import ru.mtuci.demo.repo.LicenseRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.*;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LicenseServiceImpl implements LicenseService {

    private final LicenseRepository licenseRepository;
    private final ProductService productService;
    private final UserService userService;
    private final LicenseTypeService licenseTypeService;
    private final LicenseHistoryService licenseHistoryService;

    @Override
    public void add(License license) {
        licenseRepository.save(license);
    }

    @Override
    public List<License> getAll() {
        return licenseRepository.findAll();
    }

    @Override
    public License getById(Long id) {
        return licenseRepository.findById(id).orElse(new License());
    }

    @Override
    public License getByKey(String key) {
        return licenseRepository.findByKey(key).orElse(new License());
    }

    public Date calculateEndDate(LicenseType licenseType) {
        Calendar calendar = Calendar.getInstance();

        Integer duration = licenseType.getDefaultDuration();

        if (duration == null || duration <= 0) {
            duration = 1;
        }

        calendar.add(Calendar.MONTH, duration);

        return calendar.getTime();
    }


    @Override
    public License createLicense(Long productId, Long ownerId, Long licenseTypeId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new ProductNull();
        }

        User user = userService.getById(ownerId);
        if (user == null) {
            throw new UserNull();
        }

        LicenseType licenseType = licenseTypeService.getLicenseTypeById(licenseTypeId);
        if (licenseType == null) {
            throw new TypeofLicenseNull();
        }

        License license = new License();
        license.setProduct(product);
        license.setOwner(user);
        license.setLicenseType(licenseType);

        String activationCode;
        do {
            activationCode = UUID.randomUUID().toString();
        } while (licenseRepository.existsByKey(activationCode));
        license.setActivationCode(activationCode);

        license.setEndDate(calculateEndDate(licenseType));

        licenseRepository.save(license);

        licenseHistoryService.recordLicenseChange(license, user, "Создана", "Лицензия успешно создана");

        return license;
    }
}
