package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.controller.requests.LicenseActivationRequest;
import ru.mtuci.demo.controller.requests.UpdateLicenseRequest;
import ru.mtuci.demo.exception.ProductNull;
import ru.mtuci.demo.exception.TypeofLicenseNull;
import ru.mtuci.demo.exception.UserNull;
import ru.mtuci.demo.model.*;
import ru.mtuci.demo.repo.DeviceLicenseRepository;
import ru.mtuci.demo.repo.LicenseRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.*;
import ru.mtuci.demo.ticket.Ticket;
import ru.mtuci.demo.ticket.TicketS;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LicenseServiceImpl implements LicenseService {

    private final LicenseRepository licenseRepository;
    private final ProductService productService;
    private final UserService userService;
    private final LicenseTypeService licenseTypeService;
    private final LicenseHistoryService licenseHistoryService;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final UserRepository userRepository;
    private final DeviceService deviceService;
    private final TicketS ticketS;

    @Override
    public void add(License license) {
        licenseRepository.save(license);
    }

    public boolean existsByLicenseTypeId(Long licenseTypeId) {
        return licenseRepository.existsByLicenseTypeId(licenseTypeId);
    }

    public boolean existsByProductId(Long productId) {
        return licenseRepository.existsByProductId(productId);
    }

    public License findById(Long id) {
        return licenseRepository.findById(id).orElse(null);
    }

    public void deleteById(Long id) {
        licenseRepository.deleteById(id);
    }

    @Override
    public License getByKey(String key) {
        return licenseRepository.findByKey(key).orElse(null);
    }

    public Ticket activateLicense(LicenseActivationRequest request, User authenticatedUser) {
        User user = userRepository.findById(request.getDeviceRequest().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        License license = getByKey(request.getKey());
        if (license == null) {
            throw new IllegalArgumentException("Лицензия не найдена");
        }

        if (license.getUser() != null && !license.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Невозможно активировать лицензию на другого пользователя");
        } else {
            license.setUser(user);
        }

        long activeDeviceCount = countActiveDevicesForLicense(license);
        if (activeDeviceCount >= license.getMaxDevices()) {
            throw new IllegalArgumentException("Превышено максимальное количество устройств для данной лицензии");
        }

        Device existingDevice = deviceService.getByNameForUser(request.getDeviceRequest().getName(), user.getId());
        Device device;

        if (existingDevice != null) {

            device = existingDevice;
        } else {
            device = deviceService.registerOrUpdateDevice(request.getDeviceRequest(), user);
        }

        boolean isAlreadyLinked = deviceLicenseRepository
                .findByLicenseIdAndDeviceId(license.getId(), device.getId())
                .isPresent();
        if (isAlreadyLinked) {
            throw new IllegalArgumentException("Устройство уже связано с этой лицензией");
        }

        DeviceLicense deviceLicense = new DeviceLicense();
        deviceLicense.setLicense(license);
        deviceLicense.setDevice(device);
        deviceLicense.setActivationDate(new Date());
        deviceLicenseRepository.save(deviceLicense);

        Integer defaultDuration = license.getLicenseType().getDefaultDuration();
        Date newExpiration = Date.from(
                LocalDate.now()
                        .plusMonths(defaultDuration)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        if (license.getExpirationDate() == null) {
            license.setExpirationDate(newExpiration);
        }

        updateLicense(license, authenticatedUser);

        licenseHistoryService.recordLicenseChange(license, authenticatedUser, "Activated", "Лицензия успешно активирована");
        Ticket ticket = new Ticket(license,device);
        return ticket;
    }

    @Override
    public License createLicense(Long productId, Long ownerId, Long licenseTypeId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new ProductNull();
        }

        User user = userService.getById(ownerId);
        if (user == null) {
            throw new UserNull();
        }

        LicenseType licenseType = licenseTypeService.getLicenseTypeById(licenseTypeId);
        if (licenseType == null) {
            throw new TypeofLicenseNull();
        }

        License license = new License();
        license.setProduct(product);
        license.setOwner(user);
        license.setLicenseType(licenseType);
        license.setMaxDevices(licenseType.getMaxDevices());
        license.setFlagForBlocked(false);

        String activationCode;
        do {
            activationCode = UUID.randomUUID().toString();
        } while (licenseRepository.existsByKey(activationCode));
        license.setKey(activationCode);

        licenseRepository.save(license);

        licenseHistoryService.recordLicenseChange(license, user, "Создана", "Лицензия успешно создана");

        return license;
    }

    @Override
    public void updateLicense(License license, User user) {
        license.setBlocked(false);

        if (license.getActivationDate() == null) {
            license.setActivationDate(new Date());
        }
        license.setBlocked(false);

        licenseRepository.save(license);
    }

    public List<License> getActiveLicensesForUser(User user) {
        return licenseRepository.findByUserAndActivationDateNotNullAndExpirationDateAfter(user, new Date());
    }

    public Ticket renewLicense(UpdateLicenseRequest updateLicenseRequest, User authenticatedUser) {
        boolean isAdmin = authenticatedUser.getRole() == ApplicationRole.ADMIN;

        License oldLicense = getByKey(updateLicenseRequest.getOldLicenseKey());
        if (oldLicense == null) {
            throw new IllegalArgumentException("Старая лицензия не найдена по указанному ключу");
        }

        User licenseOwner = oldLicense.getUser();
        if (!isAdmin && (licenseOwner == null || !licenseOwner.getEmail().equals(authenticatedUser.getEmail()))) {
            throw new IllegalArgumentException("Вы не можете продлевать чужую лицензию");
        }

        License newLicense = getByKey(updateLicenseRequest.getLicenseKey());
        if (newLicense == null) {
            throw new IllegalArgumentException("Новая лицензия не найдена по указанному ключу");
        }

        if (newLicense.getActivationDate() != null) {
            throw new IllegalArgumentException("Новая лицензия уже активирована");
        }

        Integer oldMaxDevices = oldLicense.getLicenseType().getMaxDevices();
        Integer newMaxDevices = newLicense.getLicenseType().getMaxDevices();
        if (oldMaxDevices > newMaxDevices) {
            throw new IllegalArgumentException("Новая лицензия не поддерживает текущее количество устройств. " +
                    "Максимум для старой лицензии: " + oldMaxDevices +
                    ", максимум для новой лицензии: " + newMaxDevices);
        }

        long activeDeviceCount = countActiveDevicesForLicense(newLicense);
        if (activeDeviceCount >= newMaxDevices) {
            throw new IllegalArgumentException("Превышено максимальное количество устройств для новой лицензии");
        }

        Integer defaultDuration = newLicense.getLicenseType().getDefaultDuration();
        Date newExpiration;

        if (oldLicense.getExpirationDate() == null) {
            newExpiration = Date.from(
                    LocalDate.now()
                            .plusMonths(defaultDuration)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
            );
        } else {
            newExpiration = Date.from(
                    oldLicense.getExpirationDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .plusMonths(defaultDuration)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
            );
        }

        oldLicense.setBlocked(true);
        oldLicense.setFlagForBlocked(true);
        update(oldLicense);

        List<DeviceLicense> deviceLicenses = deviceLicenseRepository.findByLicenseId(oldLicense.getId());
        for (DeviceLicense dl : deviceLicenses) {
            dl.setLicense(newLicense);
            deviceLicenseRepository.save(dl);
        }

        newLicense.setUser(licenseOwner);
        newLicense.setBlocked(false);
        newLicense.setActivationDate(new Date());
        newLicense.setExpirationDate(newExpiration);
        add(newLicense);

        licenseHistoryService.recordLicenseChange(
                newLicense,
                licenseOwner,
                "Renewed",
                "Лицензия была успешно продлена. Новая дата окончания: " + newExpiration
        );
        Ticket ticket = new Ticket(newLicense,deviceLicenses.get(0).getDevice());
        return ticket;
    }


    @Override
    public void update(License license) {
        if (license == null || license.getId() == null) {
            throw new IllegalArgumentException("Лицензия или её ID не может быть null");
        }
        licenseRepository.save(license);
    }

    public long countActiveDevicesForLicense(License license) {

        return deviceLicenseRepository.countByLicenseAndActivationDateIsNotNull(license);
    }

    public List<License> getByProduct(Product product) {
        return licenseRepository.findByProduct(product);
    }
}
