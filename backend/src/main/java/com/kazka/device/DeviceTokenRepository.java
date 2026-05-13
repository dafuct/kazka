package com.kazka.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {
    Optional<DeviceToken> findByDeviceToken(String deviceToken);
    List<DeviceToken> findByUserId(String userId);
    void deleteByDeviceToken(String deviceToken);
}
