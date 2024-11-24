package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.controller.requests.DeviceRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.services.DeviceService;
import ru.mtuci.demo.services.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<String> registerOrUpdateDevice(@RequestBody DeviceRequest deviceRequest, @RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("Пользователь не найден");
        }

        Device device = deviceService.registerOrUpdateDevice(deviceRequest);
        return ResponseEntity.ok("Устройство зарегистрировано или обновлено: ID " + device.getId());
    }
}
