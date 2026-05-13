package com.kazka.device;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.device.dto.DeviceRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
public class DevicesController {

    private final DeviceTokenRepository repository;
    private final CurrentUserResolver currentUserResolver;

    public DevicesController(DeviceTokenRepository repository, CurrentUserResolver currentUserResolver) {
        this.repository = repository;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> register(@RequestBody @Valid DeviceRegisterRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.<Void>fromRunnable(() -> upsert(cu.userId(), req))
                        .subscribeOn(Schedulers.boundedElastic())).then();
    }

    @DeleteMapping("/{deviceToken}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> unregister(@PathVariable String deviceToken) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.<Void>fromRunnable(() -> repository.deleteByDeviceToken(deviceToken))
                        .subscribeOn(Schedulers.boundedElastic())).then();
    }

    private void upsert(String userId, DeviceRegisterRequest req) {
        var existing = repository.findByDeviceToken(req.deviceToken());
        DeviceToken row = existing.orElseGet(DeviceToken::new);
        if (row.getId() == null) row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setDeviceToken(req.deviceToken());
        row.setPlatform(req.platform());
        row.setLocale(req.locale());
        repository.save(row);
    }
}
