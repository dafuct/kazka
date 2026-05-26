package com.kazka.holidays;

import com.kazka.holidays.dto.HolidayDto;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayCalendar calendar;

    public HolidayController(HolidayCalendar calendar) {
        this.calendar = calendar;
    }

    @GetMapping("/today")
    public Mono<ResponseEntity<HolidayDto>> today(@RequestParam @NotBlank String tz,
                                                  @RequestParam(defaultValue = "uk") String lang) {
        ZoneId zone;
        try { zone = ZoneId.of(tz); }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_timezone");
        }

        Instant now = Instant.now();
        Optional<Holiday> active = calendar.activeFor(now, zone);
        if (active.isEmpty()) {
            return Mono.just(ResponseEntity.noContent().build());
        }
        Holiday h = active.get();
        LocalDate today = now.atZone(zone).toLocalDate();
        LocalDate hday;
        try { hday = h.dateRule().computeFor(today.getYear()); }
        catch (Exception ex) { hday = today; }
        Instant date = hday.atStartOfDay(zone).toInstant();

        HolidayDto dto = new HolidayDto(
                h.id(),
                h.label(lang),
                h.label(lang),
                date);
        return Mono.just(ResponseEntity.ok(dto));
    }
}
