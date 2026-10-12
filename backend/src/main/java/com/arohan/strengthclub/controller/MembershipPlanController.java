package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.AdminDtos.MembershipPlanResponse;
import com.arohan.strengthclub.service.MembershipService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lists active membership plans that prospective members may review. */
@RestController
@RequestMapping("/api/membership-plans")
public class MembershipPlanController {

    private final MembershipService membershipService;

    public MembershipPlanController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<MembershipPlanResponse> list() {
        return membershipService.getActivePlans();
    }
}
