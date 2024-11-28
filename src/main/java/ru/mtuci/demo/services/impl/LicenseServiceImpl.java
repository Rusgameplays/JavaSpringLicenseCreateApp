package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.exception.ProductNull;
import ru.mtuci.demo.exception.TypeofLicenseNull;
import ru.mtuci.demo.exception.UserNull;
import ru.mtuci.demo.model.*;
import ru.mtuci.demo.repo.DeviceLicenseRepository;
import ru.mtuci.demo.repo.DeviceRepository;
import ru.mtuci.demo.repo.LicenseRepository;
import ru.mtuci.demo.services.*;
import ru.mtuci.demo.ticket.Ticket;
import ru.mtuci.demo.ticket.TicketSigner;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
    public List<License> getAll() {
        return licenseRepository.findAll();
    }

    @Override
    public License getById(Long id) {
        return licenseRepository.findById(id).orElse(new License());
    }

    @Override
    public License getByKey(String key) {
        return licenseRepository.findByKey(key).orElse(null);
    }

    public Date calculateEndDate(LicenseType licenseType) {
        Calendar calendar = Calendar.getInstance();

        Integer duration = licenseType.getDefaultDuration();

        if (duration == null || duration <= 0) {
            duration = 1;
        }

        calendar.add(Calendar.MONTH, duration);

        return calendar.getTime();
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

        //TODO: наверно ещё рановато дату окончания считать
        license.setExpirationDate(calculateEndDate(licenseType));

        licenseRepository.save(license);

        licenseHistoryService.recordLicenseChange(license, user, "Создана", "Лицензия успешно создана");

        return license;
    }


    @Override
    public void updateLicense(License license, User user) {
        //TODO: владелец при активации меняться не должен
        license.setOwner(user);
        license.setBlocked(false);
        //TODO: а если лицензия активируется не в первый раз?
        license.setActivationDate(new Date());
        license.setBlocked(false);

        licenseRepository.save(license);
    }

    public List<License> getActiveLicensesForUser(User user) {
        return licenseRepository.findByUserAndActivationDateNotNullAndExpirationDateAfter(user, new Date());
    }

    @Override
    public Ticket generateTicket(License license, Device device) {

        Ticket ticket = new Ticket();
        ticket.setServerDate(new Date());
        ticket.setTicketLifetime(license.getLicenseType().getDefaultDuration() != null
                ? license.getLicenseType().getDefaultDuration().longValue() * 30 * 24 * 60 * 60 * 1000
                : 0L);
        ticket.setActivationDate(license.getActivationDate());
        ticket.setExpirationDate(license.getExpirationDate());
        if (device.getUser() != null) {
            ticket.setUserId(device.getUser().getId());
        } else {
            ticket.setUserId(null);
        }
        ticket.setDeviceId(device.getMac());
        ticket.setLicenseBlocked(license.getBlocked() != null ? license.getBlocked().toString() : "null");

        String serializedTicket = TicketSigner.serializeTicket(ticket);
        //TODO: хорошо бы сделать так, чтобы тикет сам её генерировал при создании объекта
        String digitalSignature = Ticket.DigitalSignatureUtil.generateSignature(serializedTicket);
        ticket.setDigitalSignature(digitalSignature);

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
