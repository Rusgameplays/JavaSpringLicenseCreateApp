package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.exception.ProductNull;
import ru.mtuci.demo.exception.TypeofLicenseNull;
import ru.mtuci.demo.exception.UserNull;
import ru.mtuci.demo.model.*;
import ru.mtuci.demo.repo.LicenseRepository;
import ru.mtuci.demo.services.*;
import ru.mtuci.demo.ticket.Ticket;
import ru.mtuci.demo.ticket.TicketSigner;

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

    @Override
    public void add(License license) {
        licenseRepository.save(license);
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

        String activationCode;
        do {
            activationCode = UUID.randomUUID().toString();
        } while (licenseRepository.existsByKey(activationCode));
        license.setKey(activationCode);

        license.setExpirationDate(calculateEndDate(licenseType));

        licenseRepository.save(license);

        licenseHistoryService.recordLicenseChange(license, user, "Создана", "Лицензия успешно создана");

        return license;
    }

    @Override
    public boolean validateActivation(License license, Device device, User user) {
        if (license.getBlocked() || license.getExpirationDate().before(new Date())) {
            return false;
        }
        return true;
    }

    @Override
    public void updateLicense(License license, User user) {
        license.setOwner(user);
        license.setBlocked(false);
        license.setActivationDate(new Date());
        license.setBlocked(false);

        licenseRepository.save(license);
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
        ticket.setUserId(license.getOwner().getId());
        ticket.setDeviceId(device.getMac());
        ticket.setLicenseBlocked(license.getBlocked() != null ? license.getBlocked().toString() : "null");

        String serializedTicket = TicketSigner.serializeTicket(ticket);
        String digitalSignature = Ticket.DigitalSignatureUtil.generateSignature(serializedTicket);
        ticket.setDigitalSignature(digitalSignature);

        return ticket;
    }



}
