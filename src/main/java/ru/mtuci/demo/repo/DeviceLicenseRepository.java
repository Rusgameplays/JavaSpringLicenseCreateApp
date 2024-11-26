package ru.mtuci.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.demo.model.DeviceLicense;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {


    long countByLicenseAndActivationDateIsNotNull(License license);
}
