package ru.mtuci.demo.services;

import ru.mtuci.demo.controller.requests.DeviceRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.User;

public interface DeviceService {
    Device registerOrUpdateDevice(DeviceRequest deviceRequest, User user);
    Device getByMac(String mac);
    Device getByNameForUser(String name, Long userId);
}

