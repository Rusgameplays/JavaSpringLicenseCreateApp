package ru.mtuci.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.Product;
import ru.mtuci.demo.model.User;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByKey(String key);
    boolean existsByKey(String key);
    List<License> findByUserAndActivationDateNotNullAndExpirationDateAfter(User owner, Date currentDate);
    void delete(License license);
    List<License> findByProduct(Product product);
}
