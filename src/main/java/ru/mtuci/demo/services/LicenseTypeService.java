package ru.mtuci.demo.services;

import ru.mtuci.demo.model.LicenseType;

import java.util.List;

public interface LicenseTypeService {
    LicenseType getLicenseTypeById(Long id);
    LicenseType addLicenseType(LicenseType licenseType);
    List<LicenseType> getAllLicenseTypes();


}
