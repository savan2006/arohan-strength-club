package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AttendanceDtos.GymStatusResponse;
import com.arohan.strengthclub.entity.GymClosure;
import com.arohan.strengthclub.entity.GymOperatingHour;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.repository.GymClosureRepository;
import com.arohan.strengthclub.repository.GymOperatingHourRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facility hours and closure status.
 *
 * <p>Hours and closures change rarely but are read on almost every request (landing page, dashboard,
 * check-in, session auto-close). They are cached briefly in memory so these reads do not each cost a
 * database round trip. Admin edits invalidate the cache after the transaction commits.
 */
@Service
public class GymService {
    private static final long TTL_NANOS = 30_000_000_000L; // 30 seconds
    private static final int MAX_CACHED_DATES = 32;

    private record Timed<T>(T value, long expiresAt) {
        boolean fresh() { return System.nanoTime() < expiresAt; }
    }

    private final GymOperatingHourRepository hoursRepository;
    private final GymClosureRepository closureRepository;
    private final ZoneId zoneId;
    private final Map<String, Timed<Optional<GymOperatingHour>>> hoursCache = new ConcurrentHashMap<>();
    private final Map<LocalDate, Timed<Optional<GymClosure>>> closureCache = new ConcurrentHashMap<>();

    public GymService(GymOperatingHourRepository hoursRepository, GymClosureRepository closureRepository,
                      @Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.hoursRepository = hoursRepository;
        this.closureRepository = closureRepository;
        this.zoneId = ZoneId.of(timezone);
    }

    public GymStatusResponse getStatus() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate date = now.toLocalDate();
        String day = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        GymOperatingHour hours = cachedHours(day);
        GymClosure closure = cachedClosure(date);
        if (closure != null) {
            return new GymStatusResponse(false, closure.getAnnouncement().isBlank() ? closure.getReason() : closure.getAnnouncement(),
                day, date, hours == null ? null : hours.getOpeningTime(), hours == null ? null : hours.getClosingTime(),
                closure.getReason(), closure.getAnnouncement());
        }
        if (hours == null || !hours.isOpen()) {
            return new GymStatusResponse(false, "The facility is closed today.", day, date,
                hours == null ? null : hours.getOpeningTime(), hours == null ? null : hours.getClosingTime(), null, null);
        }
        LocalTime time = now.toLocalTime();
        boolean openNow = !time.isBefore(hours.getOpeningTime()) && time.isBefore(hours.getClosingTime());
        String message = openNow ? "Open now · closes at " + hours.getClosingTime() :
            (time.isBefore(hours.getOpeningTime()) ? "Opens at " + hours.getOpeningTime() : "Closed for today · opens next operating day");
        return new GymStatusResponse(openNow, message, day, date, hours.getOpeningTime(), hours.getClosingTime(), null, null);
    }

    public GymOperatingHour getHoursFor(LocalDate date) {
        String day = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return cachedHours(day);
    }

    public void requireOpenForCheckIn() {
        GymStatusResponse status = getStatus();
        if (!status.open()) {
            throw new BusinessRuleException(status.closureReason() != null ? "GYM_CLOSED_FOR_DATE" : "GYM_CLOSED",
                status.message(), HttpStatus.BAD_REQUEST);
        }
    }

    public ZoneId getZoneId() { return zoneId; }

    /** Drops cached hours/closures once the surrounding transaction (if any) has committed. */
    public void invalidateCacheAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { invalidateCache(); }
            });
        } else {
            invalidateCache();
        }
    }

    public void invalidateCache() {
        hoursCache.clear();
        closureCache.clear();
    }

    private GymOperatingHour cachedHours(String day) {
        Timed<Optional<GymOperatingHour>> entry = hoursCache.get(day);
        if (entry == null || !entry.fresh()) {
            entry = new Timed<>(hoursRepository.findByDayOfWeekIgnoreCase(day), System.nanoTime() + TTL_NANOS);
            hoursCache.put(day, entry);
        }
        return entry.value().orElse(null);
    }

    private GymClosure cachedClosure(LocalDate date) {
        Timed<Optional<GymClosure>> entry = closureCache.get(date);
        if (entry == null || !entry.fresh()) {
            if (closureCache.size() >= MAX_CACHED_DATES) {
                closureCache.clear();
            }
            entry = new Timed<>(closureRepository.findByDate(date), System.nanoTime() + TTL_NANOS);
            closureCache.put(date, entry);
        }
        return entry.value().orElse(null);
    }
}
