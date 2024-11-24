package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.controller.requests.DeviceRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.DeviceRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.DeviceService;

import java.util.Random;

@RequiredArgsConstructor
@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Override
    public Device registerOrUpdateDevice(DeviceRequest deviceRequest) {
        User user = userRepository.findById(deviceRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        String macAddress = getLocalMacAddress();

        Device device = new Device();

        device.setMac(macAddress);
        device.setName(deviceRequest.getName());
        device.setUser(user);

        return deviceRepository.save(device);
    }
    private String getLocalMacAddress() {
        String macAddress;
        do {
            macAddress = generateRandomMacAddress();
        } while (deviceRepository.existsByMac(macAddress));

        return macAddress;
    }

    private String generateRandomMacAddress() {
        Random random = new Random();
        byte[] macBytes = new byte[6];

        macBytes[0] = (byte) (random.nextInt(256) & 0xFE);
        for (int i = 1; i < 6; i++) {
            macBytes[i] = (byte) random.nextInt(256);
        }

        StringBuilder macAddress = new StringBuilder();
        for (byte b : macBytes) {
            macAddress.append(String.format("%02X:", b));
        }
        macAddress.setLength(macAddress.length() - 1);
        return macAddress.toString();
    }

}