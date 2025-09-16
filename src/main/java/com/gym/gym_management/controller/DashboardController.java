package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.ActivityDto;
import com.gym.gym_management.controller.dto.DashboardStatsDto;
import com.gym.gym_management.service.ActivityService;
import com.gym.gym_management.service.ClientService;
import com.gym.gym_management.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final ClientService clientService;
    private final PaymentService paymentService;
    private final ActivityService activityService;

    public DashboardController(ClientService clientService, PaymentService paymentService, ActivityService activityService) {
        this.clientService = clientService;
        this.paymentService = paymentService;
        this.activityService = activityService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        long activeClients = clientService.countActiveClients();
        long expiredPayments = paymentService.countExpiredPayments();

        DashboardStatsDto stats = new DashboardStatsDto(activeClients, expiredPayments);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityDto>> getRecentActivities(@RequestParam(defaultValue = "10") int limit) {
        List<ActivityDto> activities = activityService.getRecentActivities(limit);
        return ResponseEntity.ok(activities);
    }
}
