package com.kazka.dashboard;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.dashboard.dto.DashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
public class DashboardController {

    private final DashboardService svc;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/api/dashboard")
    public Mono<DashboardDto> get() {
        return currentUserResolver.requireUser().flatMap(svc::getDashboard);
    }
}
