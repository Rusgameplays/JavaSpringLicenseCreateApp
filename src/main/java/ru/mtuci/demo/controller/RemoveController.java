package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.LicenseRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.LicenseHistoryService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.services.LicenseTypeService;
import ru.mtuci.demo.services.ProductService;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/remove")
@PreAuthorize("hasRole('ADMIN')")
public class RemoveController {

    private final LicenseTypeService licenseTypeService;
    private final ProductService productService;
    private final LicenseService licenseService;
    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;

    @DeleteMapping("/license-type/{id}")
    public ResponseEntity<?> removeLicenseType(@PathVariable Long id) {
        try {
            if (licenseService.existsByLicenseTypeId(id)) {
                return ResponseEntity.badRequest().body("Невозможно удалить LicenseType, так как существуют лицензии, использующие этот тип.");
            }
            licenseTypeService.deleteById(id);
            return ResponseEntity.ok("LicenseType успешно удалён.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<?> removeProduct(@PathVariable Long id) {
        try {
            if (licenseService.existsByProductId(id)) {
                return ResponseEntity.badRequest().body("Невозможно удалить Product, так как существуют лицензии, использующие этот продукт.");
            }
            productService.deleteById(id);
            return ResponseEntity.ok("Product успешно удалён.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }

    @DeleteMapping("/license/{id}")
    public ResponseEntity<?> removeLicense(@PathVariable Long id) {
        try {
            License license = licenseService.findById(id);
            if (license == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Лицензия не найдена.");
            }

            if (license.getActivationDate() != null) {
                return ResponseEntity.badRequest().body("Невозможно удалить активированную лицензию.");
            }


//            licenseHistoryService.recordLicenseChange(
//                    license,
//                    license.getUser(),
//                    "Deleted",
//                    "Лицензия была удалена администратором."
//            ); НЕТ СМЫСЛА - УДАЛЯЕТСЯ

            licenseService.deleteById(id);
            return ResponseEntity.ok("License успешно удалена.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> removeUser(@PathVariable Long userId) {
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь с ID " + userId + " не найден");
            }

            User user = userOptional.get();

            boolean hasLicenses = licenseRepository.existsByUserId(userId);
            if (hasLicenses) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Нельзя удалить пользователя с активными или завершенными лицензиями");
            }

            userRepository.delete(user);

            return ResponseEntity.ok("Пользователь с ID " + userId + " успешно удален");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении пользователя: " + e.getMessage());
        }
    }

}
