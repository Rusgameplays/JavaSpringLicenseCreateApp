package ru.mtuci.demo.services;

import ru.mtuci.demo.controller.requests.LicenseActivationRequest;
import ru.mtuci.demo.controller.requests.UpdateLicenseRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.Product;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.ticket.Ticket;

import java.util.List;

public interface LicenseService {
    void add(License license);
    License getByKey(String key);
    void updateLicense(License license, User user);
    License createLicense(Long productId, Long ownerId, Long licenseTypeId);
    List<License> getActiveLicensesForUser(User authenticatedUser);
    void update(License license);
    List<License> getByProduct(Product product);
    long countActiveDevicesForLicense(License license);
    void deleteById(Long id);
    License findById(Long id);
    boolean existsByProductId(Long id);
    boolean existsByLicenseTypeId(Long id);
    Ticket activateLicense(LicenseActivationRequest request, User authenticatedUser);
    Ticket renewLicense(UpdateLicenseRequest updateLicenseRequest, User authenticatedUser);
}
