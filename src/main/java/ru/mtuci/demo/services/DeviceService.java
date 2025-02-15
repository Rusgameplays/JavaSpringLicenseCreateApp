package ru.mtuci.demo.services;

import ru.mtuci.demo.controller.requests.LicenseActivationRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.User;

public interface DeviceService {
    Device registerOrUpdateDevice(LicenseActivationRequest deviceRequest, User user);
    Device getByMac(String mac);
    Device getByNameForUser(String name, Long userId);
}

