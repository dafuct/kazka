package com.kazka.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {
    Optional<DeviceToken> findByDeviceToken(String deviceToken);
    List<DeviceToken> findByUserId(String userId);

    @Transactional
    void deleteByDeviceToken(String deviceToken);

    @Transactional
    void deleteByDeviceTokenAndUserId(String deviceToken, String userId);
}
