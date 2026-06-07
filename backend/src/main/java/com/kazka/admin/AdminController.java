package com.kazka.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Mono<List<AdminUserDto>> listUsers() {
        return Mono.fromCallable(adminService::listUsers).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/users/{id}/unsuspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> unsuspend(@PathVariable String id) {
        return Mono.fromRunnable(() -> adminService.unsuspend(id))
                .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @PostMapping("/stories/{id}/showcase")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> setShowcase(@PathVariable String id,
                                  @RequestParam(defaultValue = "true") boolean on) {
        return Mono.fromRunnable(() -> adminService.setShowcase(id, on))
                .subscribeOn(Schedulers.boundedElastic()).then();
    }
}
