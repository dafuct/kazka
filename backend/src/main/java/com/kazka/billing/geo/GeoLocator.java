package com.kazka.billing.geo;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class GeoLocator {

    private static final Pattern ISO2 = Pattern.compile("^[A-Z]{2}$");
    private static final String DEFAULT = "US";

    /**
     * Detects ISO-3166 alpha-2 country code from edge proxy headers.
     * cfHeader: Cloudflare's CF-IPCountry header (preferred).
     * xCountry: generic X-Country header set by other reverse proxies.
     * override: user-supplied query param ?country=XX to force a value.
     */
    public String detect(String cfHeader, String xCountry, String override) {
        for (String candidate : new String[]{override, cfHeader, xCountry}) {
            if (candidate == null) continue;
            String normalized = candidate.trim().toUpperCase();
            if (ISO2.matcher(normalized).matches()) return normalized;
        }
        return DEFAULT;
    }

    /** Convenience: is this a Ukrainian user? */
    public boolean isUkraine(String country) {
        return "UA".equalsIgnoreCase(country);
    }
}
