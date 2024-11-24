package ru.mtuci.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.demo.model.Device;

import java.util.List;
import java.util.Optional;


public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByMac(String mac);
    boolean existsByMac(String mac);
    List<Device> findAllByUserId(Long userId);
}
