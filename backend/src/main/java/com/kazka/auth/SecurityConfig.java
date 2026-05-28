package com.kazka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.auth.token.BearerTokenAuthenticationWebFilter;
import com.kazka.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsRepositoryReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService uds, PasswordEncoder pe) {
        var mgr = new UserDetailsRepositoryReactiveAuthenticationManager(uds);
        mgr.setPasswordEncoder(pe);
        return mgr;
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(
            ServerHttpSecurity http,
            UserDetailsRepositoryReactiveAuthenticationManager authManager,
            OAuth2SuccessHandler oauthSuccess,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            BearerTokenAuthenticationWebFilter bearerFilter,
            ObjectProvider<CorsConfigurationSource> corsProvider) {

        var loginFilter = new AuthenticationWebFilter(authManager);
        loginFilter.setRequiresAuthenticationMatcher(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/auth/login"));
        loginFilter.setServerAuthenticationConverter(new JsonLoginConverter(objectMapper));
        var success = new JsonLoginSuccessHandler(objectMapper);
        success.setUsers(userRepository);
        loginFilter.setAuthenticationSuccessHandler(success);
        loginFilter.setAuthenticationFailureHandler((webFilterExchange, exception) ->
                writeError(webFilterExchange.getExchange(), HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", objectMapper));

        return http
                .cors(c -> {
                    CorsConfigurationSource src = corsProvider.getIfAvailable();
                    if (src != null) c.configurationSource(src); else c.disable();
                })
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(new AndServerWebExchangeMatcher(
                                CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                                new NegatedServerWebExchangeMatcher(
                                        ServerWebExchangeMatchers.pathMatchers(
                                                "/oauth2/**", "/login/oauth2/**",
                                                "/api/auth/signup", "/api/auth/login",
                                                "/api/auth/password-reset/**",
                                                "/api/auth/token/**",
                                                "/api/auth/oauth/**",
                                                "/api/devices/**",
                                                // Webhook endpoints use signed payloads from payment
                                                // providers (no browser session / no CSRF token).
                                                // IAP endpoints are called by iOS native clients
                                                // (Bearer-token only, no cookie session).
                                                // /api/billing/checkout-session is deliberately
                                                // excluded here so it keeps CSRF protection.
                                                "/api/billing/webhook/**",
                                                "/api/billing/iap/**")))))
                .authorizeExchange(auth -> auth
                        // Illustrations are linked to specific stories. We gate uploads behind
                        // auth so unauth users can't enumerate `/uploads/{storyId}-{theme}.png`.
                        // (Residual gap: any authenticated user can still fetch any storyId they
                        // know; full IDOR closure requires a proxy endpoint that checks ownership.)
                        .pathMatchers(HttpMethod.GET, "/uploads/**").authenticated()
                        .pathMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/password-reset/request",
                                "/api/auth/password-reset/confirm",
                                "/api/auth/token/login",
                                "/api/auth/token/refresh",
                                "/api/auth/token/logout",
                                "/api/auth/oauth/apple",
                                "/api/auth/oauth/google").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/auth/me", "/api/auth/verify-email").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/billing/products").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/holidays/**").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/billing/iap/webhook",
                                "/api/billing/webhook/**").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterAt(loginFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(bearerFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .oauth2Login(o -> o.authenticationSuccessHandler(oauthSuccess))
                .logout(l -> l.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((webFilterExchange, authentication) -> {
                            webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                            return Mono.empty();
                        }))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((swe, ex) ->
                                writeError(swe, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", objectMapper))
                        .accessDeniedHandler((swe, ex) ->
                                writeError(swe, HttpStatus.FORBIDDEN, "FORBIDDEN", objectMapper)))
                .formLogin(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .build();
    }

    @Bean
    public WebFilter csrfTokenAttributeFilter() {
        return (exchange, chain) -> {
            Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
            if (token != null) return token.then(chain.filter(exchange));
            return chain.filter(exchange);
        };
    }

    @Profile("dev")
    @Bean
    public CorsConfigurationSource corsConfigSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    private static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status,
                                         String code, ObjectMapper mapper) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        try {
            byte[] body = mapper.writeValueAsBytes(Map.of("error", code));
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(body)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
