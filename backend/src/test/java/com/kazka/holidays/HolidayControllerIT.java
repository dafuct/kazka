package com.kazka.holidays;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("integration")
class HolidayControllerIT extends AbstractIT {

    @MockitoBean HolidayCalendar calendar;

    @Test
    void should_return_200_with_dto_when_holiday_active() {
        when(calendar.activeFor(any(), any())).thenReturn(Optional.of(Holiday.CHRISTMAS));
        client().get().uri("/api/holidays/today?tz=Europe/Kyiv").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("christmas")
                .jsonPath("$.name").isEqualTo("Різдво");
    }

    @Test
    void should_return_204_when_no_holiday_active() {
        when(calendar.activeFor(any(), any())).thenReturn(Optional.empty());
        client().get().uri("/api/holidays/today?tz=Europe/Kyiv").exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_return_400_on_invalid_timezone() {
        client().get().uri("/api/holidays/today?tz=Mars/Olympus").exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void should_respect_lang_query_param() {
        when(calendar.activeFor(any(), any())).thenReturn(Optional.of(Holiday.CHRISTMAS));
        client().get().uri("/api/holidays/today?tz=Europe/Kyiv&lang=en").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Christmas");
    }
}
