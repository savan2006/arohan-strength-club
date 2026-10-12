package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AttendanceDtos.GymStatusResponse;
import com.arohan.strengthclub.entity.GymClosure;
import com.arohan.strengthclub.entity.GymOperatingHour;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.repository.GymClosureRepository;
import com.arohan.strengthclub.repository.GymOperatingHourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymServiceTest {
    private static final ZoneId FACILITY_ZONE = ZoneId.of("Asia/Kolkata");

    @Mock private GymOperatingHourRepository hoursRepository;
    @Mock private GymClosureRepository closureRepository;

    private GymService gymService;

    @BeforeEach
    void setUp() {
        gymService = new GymService(hoursRepository, closureRepository, FACILITY_ZONE.getId());
    }

    @Test
    void publishedClosureOverridesWeeklyOpeningHours() {
        LocalDate today = LocalDate.now(FACILITY_ZONE);
        String day = today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
        GymOperatingHour hours = openAllDay(day);
        GymClosure closure = new GymClosure();
        closure.setDate(today);
        closure.setReason("Annual maintenance");
        closure.setAnnouncement("The training floor is closed today.");

        when(hoursRepository.findByDayOfWeekIgnoreCase(day)).thenReturn(Optional.of(hours));
        when(closureRepository.findByDate(today)).thenReturn(Optional.of(closure));

        GymStatusResponse result = gymService.getStatus();

        assertFalse(result.open());
        assertEquals("Annual maintenance", result.closureReason());
        assertEquals("The training floor is closed today.", result.message());
    }

    @Test
    void identifiesFacilityAsOpenWhenConfiguredHoursCoverTheLocalDay() {
        LocalDate today = LocalDate.now(FACILITY_ZONE);
        String day = today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
        when(hoursRepository.findByDayOfWeekIgnoreCase(day)).thenReturn(Optional.of(openAllDay(day)));
        when(closureRepository.findByDate(today)).thenReturn(Optional.empty());

        GymStatusResponse result = gymService.getStatus();

        assertTrue(result.open());
    }

    @Test
    void checkInIsRejectedWhenTheWeeklyScheduleIsClosed() {
        LocalDate today = LocalDate.now(FACILITY_ZONE);
        String day = today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
        GymOperatingHour hours = openAllDay(day);
        hours.setOpen(false);
        when(hoursRepository.findByDayOfWeekIgnoreCase(day)).thenReturn(Optional.of(hours));
        when(closureRepository.findByDate(today)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> gymService.requireOpenForCheckIn());
    }

    @Test
    void repeatedStatusReadsAreServedFromTheShortLivedCache() {
        LocalDate today = LocalDate.now(FACILITY_ZONE);
        String day = today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
        when(hoursRepository.findByDayOfWeekIgnoreCase(day)).thenReturn(Optional.of(openAllDay(day)));
        when(closureRepository.findByDate(today)).thenReturn(Optional.empty());

        gymService.getStatus();
        gymService.getStatus();

        verify(hoursRepository, times(1)).findByDayOfWeekIgnoreCase(day);

        gymService.invalidateCache();
        gymService.getStatus();
        verify(hoursRepository, times(2)).findByDayOfWeekIgnoreCase(day);
    }

    private GymOperatingHour openAllDay(String day) {
        GymOperatingHour hours = new GymOperatingHour();
        hours.setDayOfWeek(day);
        hours.setOpen(true);
        hours.setOpeningTime(LocalTime.MIDNIGHT);
        hours.setClosingTime(LocalTime.of(23, 59, 59));
        return hours;
    }
}
