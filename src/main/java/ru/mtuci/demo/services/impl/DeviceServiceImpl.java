package ru.mtuci.demo.services.impl;

import jakarta.persistence.EntityNotFoundException;
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
        //TODO: при вызове этого метода уже есть юзер, которого можно использовать, лишний запрос получается
        User user = userRepository.findById(deviceRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        //TODO: Не понятно, почему сервер генерирует маки
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

    public Device findDeviceByInfo(String name, String mac) {
        return deviceRepository.findByNameAndMac(name, mac).orElse(null);
    }

    public Device getByMac(String mac) {
        return deviceRepository.findByMac(mac)
                .orElseThrow(() -> new EntityNotFoundException("Устройство с MAC-адресом " + mac + " не найдено"));
    }
}