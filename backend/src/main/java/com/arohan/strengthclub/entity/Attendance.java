package com.arohan.strengthclub.entity;

import com.arohan.strengthclub.entity.enums.AttendanceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A member's check-in session, including a snapshot of exercises at check-in time. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendances", indexes = {
        @Index(name = "idx_attendance_user_date", columnList = "user_id,facility_date"),
        @Index(name = "idx_attendance_status", columnList = "status")
})
public class Attendance {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_in_time", nullable = false)
    private Instant checkInTime = Instant.now();

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 0;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private int estimatedDurationMinutes = 45;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.ACTIVE;

    @Column(name = "facility_date", nullable = false)
    private LocalDate facilityDate;

    @Column(name = "workout_day_number")
    private Integer workoutDayNumber;

    @Column(name = "workout_day_name", length = 24)
    private String workoutDayName;

    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<AttendanceExercise> exercises = new ArrayList<>();

    public void addExercise(AttendanceExercise exercise) {
        exercises.add(exercise);
        exercise.setAttendance(this);
    }
}
