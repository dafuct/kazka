package com.kazka.billing.geo;

import com.kazka.billing.dto.GeoResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/billing")
public class GeoController {

    private final GeoLocator locator;

    public GeoController(GeoLocator locator) {
        this.locator = locator;
    }

    @GetMapping("/geo")
    public Mono<GeoResponse> geo(ServerHttpRequest req,
                                 @RequestParam(name = "country", required = false) String override) {
        String cf = req.getHeaders().getFirst("CF-IPCountry");
        String xc = req.getHeaders().getFirst("X-Country");
        String country = locator.detect(cf, xc, override);
        return Mono.just(new GeoResponse(country, locator.isUkraine(country)));
    }
}
