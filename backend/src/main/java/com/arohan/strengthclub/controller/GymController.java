package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.AttendanceDtos.GymStatusResponse;
import com.arohan.strengthclub.service.GymService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoint for current facility hours and closure status. */
@RestController
@RequestMapping("/api/gym")
public class GymController {

    private final GymService gymService;

    public GymController(GymService gymService) {
        this.gymService = gymService;
    }

    @GetMapping("/status")
    public GymStatusResponse status() {
        return gymService.getStatus();
    }
}
