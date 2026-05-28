package com.kazka.auth;

import com.kazka.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@Service
public class KazkaUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository users;

    @Override
    @NullMarked
    public Mono<UserDetails> findByUsername(String email) {
        return Mono.fromCallable(() -> users.findByEmail(email.trim().toLowerCase())
                        .map(KazkaUserDetails::new)
                        .orElseThrow(() -> new UsernameNotFoundException(email)))
                .subscribeOn(Schedulers.boundedElastic())
                .cast(UserDetails.class);
    }
}
