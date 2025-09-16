package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.ActivityDto;
import com.gym.gym_management.service.ActivityService;
import com.gym.gym_management.service.ClientService;
import com.gym.gym_management.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock private ClientService clientService;
    @Mock private PaymentService paymentService;
    @Mock private ActivityService activityService;

    @InjectMocks private DashboardController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void stats_returnsCounts() throws Exception {
        given(clientService.countActiveClients()).willReturn(3L);
        given(paymentService.countExpiredPayments()).willReturn(1L);

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeClients").value(3))
                .andExpect(jsonPath("$.expiredPayments").value(1));
    }

    @Test
    void activities_returnsList() throws Exception {
        var a = new ActivityDto("payment", "Pago recibido", "desc", LocalDateTime.now(), 10L);
        given(activityService.getRecentActivities(5)).willReturn(List.of(a));
        mockMvc.perform(get("/api/dashboard/activities").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("payment"))
                .andExpect(jsonPath("$[0].relatedId").value(10));
    }
}
