package ru.mtuci.demo.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.controller.requests.LicenseActivationRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.DeviceRepository;
import ru.mtuci.demo.services.DeviceService;

import java.util.Random;

@RequiredArgsConstructor
@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    public Device registerOrUpdateDevice(LicenseActivationRequest deviceRequest, User user) {
        //String macAddress = getLocalMacAddress();

        Device device = new Device();

        device.setMac(deviceRequest.getMac());
        device.setName(deviceRequest.getName());
        device.setUser(user);

        return deviceRepository.save(device);
    }

    //TODO: Не понятно, почему сервер генерирует маки - для отладки. Без отладки не могу проверить как работает программа
    // с другими устройствами
    /*public static String getLocalMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                if (networkInterface.isUp() && !networkInterface.isVirtual()) {
                    byte[] macAddress = networkInterface.getHardwareAddress();
                    if (macAddress != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < macAddress.length; i++) {
                            sb.append(String.format("%02X", macAddress[i]));
                            if (i < macAddress.length - 1) sb.append(":");
                        }
                        return sb.toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }*/

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

    public Device getByMac(String mac) {
        return deviceRepository.findByMac(mac)
                .orElseThrow(() -> new EntityNotFoundException("Устройство с MAC-адресом " + mac + " не найдено"));
    }
    public Device getByNameForUser(String name, Long userId) {
        return deviceRepository.findByNameAndUserId(name, userId)
                .orElse(null);
    }
}