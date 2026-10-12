package com.arohan.strengthclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalTime;
import java.util.UUID;

/** Opening hours and notes for a single day of the week. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gym_operating_hours", uniqueConstraints =
        @UniqueConstraint(name = "uk_gym_hours_day", columnNames = "day_of_week"))
public class GymOperatingHour {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "day_of_week", nullable = false, unique = true, length = 12)
    private String dayOfWeek;

    @Column(nullable = false)
    private boolean open = true;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime = LocalTime.of(6, 0);

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime = LocalTime.of(22, 0);

    @Column(nullable = false, length = 300)
    private String notes = "";
}
