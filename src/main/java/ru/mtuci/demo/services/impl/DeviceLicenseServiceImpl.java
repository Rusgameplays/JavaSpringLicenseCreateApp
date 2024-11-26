package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.DeviceLicense;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.repo.DeviceLicenseRepository;
import ru.mtuci.demo.services.DeviceLicenseService;

import java.util.Date;

@RequiredArgsConstructor
@Service
public class DeviceLicenseServiceImpl implements DeviceLicenseService {
    private final DeviceLicenseRepository deviceLicenseRepository;

    public DeviceLicense addDeviceToLicense(License license, Device device) {
        DeviceLicense deviceLicense = new DeviceLicense();
        deviceLicense.setLicense(license);
        deviceLicense.setDevice(device);
        deviceLicense.setActivationDate(new Date());
        return deviceLicenseRepository.save(deviceLicense);
    }
}
