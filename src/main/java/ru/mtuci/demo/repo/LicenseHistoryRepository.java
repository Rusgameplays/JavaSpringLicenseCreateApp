package ru.mtuci.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.demo.model.LicenseHistory;

import java.util.List;
import java.util.Optional;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, Long> {
}
