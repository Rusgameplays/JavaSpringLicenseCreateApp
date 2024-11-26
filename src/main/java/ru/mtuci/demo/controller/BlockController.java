package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.controller.requests.BlockRequest;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.Product;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.services.LicenseHistoryService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.services.ProductService;
import ru.mtuci.demo.services.UserService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/block")
@PreAuthorize("hasRole('ADMIN')")
public class BlockController {

    private final ProductService productService;
    private final UserService userService;
    private final LicenseService licenseService;
    private final LicenseHistoryService licenseHistoryService;

    @PostMapping("/product")
    public ResponseEntity<?> blockOrUnblockProduct(@RequestBody BlockRequest request) {
        try {
            Product product = productService.getById(request.getId());
            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Продукт не найден");
            }

            product.setBlocked(request.isBlock());
            productService.update(product);

            List<License> licenses = licenseService.getByProduct(product);
            for (License license : licenses) {
                if (license.getFlagForBlocked() == true) {
                    continue;
                }

                license.setBlocked(request.isBlock());
                licenseService.update(license);

                String historyMessage = request.isBlock() ? "Лицензия заблокирована" : "Лицензия разблокирована";
                licenseHistoryService.recordLicenseChange(license, null, "BlockStatusChange", historyMessage);
            }

            String status = request.isBlock() ? "заблокирован" : "разблокирован";
            return ResponseEntity.ok("Продукт " + status + " и все связанные лицензии обновлены");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + ex.getMessage());
        }
    }


    @PostMapping("/user")
    public ResponseEntity<?> blockOrUnblockUser(@RequestBody BlockRequest request) {
        try {
            User user = userService.getById(request.getId());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь не найден");
            }

            List<License> licenses = licenseService.getActiveLicensesForUser(user);
            for (License license : licenses) {
                if (license.getFlagForBlocked() == true) {
                    continue;
                }

                license.setBlocked(request.isBlock());
                licenseService.update(license);

                String historyMessage = request.isBlock() ? "Лицензия заблокирована пользователем" : "Лицензия разблокирована пользователем";
                licenseHistoryService.recordLicenseChange(license, user, "BlockStatusChange", historyMessage);
            }

            String status = request.isBlock() ? "заблокирован" : "разблокирован";
            return ResponseEntity.ok("Пользователь " + status + " и все связанные лицензии обновлены");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + ex.getMessage());
        }
    }

}
