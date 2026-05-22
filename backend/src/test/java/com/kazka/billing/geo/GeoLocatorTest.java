package com.kazka.billing.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoLocatorTest {

    private final GeoLocator locator = new GeoLocator();

    @Test
    void should_returnUA_when_cfIpCountryHeaderIsUA() {
        String country = locator.detect("UA", null, null);
        assertThat(country).isEqualTo("UA");
    }

    @Test
    void should_fallbackToXForwardedHeader_when_cfHeaderMissing() {
        String country = locator.detect(null, "DE", null);
        assertThat(country).isEqualTo("DE");
    }

    @Test
    void should_returnDefault_when_allHeadersMissing() {
        String country = locator.detect(null, null, null);
        assertThat(country).isEqualTo("US");
    }

    @Test
    void should_uppercaseAndValidate_when_lowercaseHeader() {
        assertThat(locator.detect("ua", null, null)).isEqualTo("UA");
    }

    @Test
    void should_ignoreInvalidCountryCode() {
        assertThat(locator.detect("XX1", null, null)).isEqualTo("US");
    }
}
