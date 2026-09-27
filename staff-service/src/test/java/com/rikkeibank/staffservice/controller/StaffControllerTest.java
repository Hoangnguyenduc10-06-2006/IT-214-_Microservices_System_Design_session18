package com.rikkeibank.staffservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.staffservice.dto.StaffRequest;
import com.rikkeibank.staffservice.entity.Staff;
import com.rikkeibank.staffservice.entity.StaffStatus;
import com.rikkeibank.staffservice.service.StaffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffController.class)
@AutoConfigureMockMvc(addFilters = false)
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffService staffService;

    @Test
    @DisplayName("GET /api/staffs by ADMIN should return 200")
    void testGetAllStaffByAdmin() throws Exception {
        Staff s = Staff.builder()
                .id(1L)
                .fullName("Pham Van D")
                .email("vand@rikkeibank.com")
                .position("TELLER")
                .status(StaffStatus.ACTIVE)
                .build();

        when(staffService.getAllStaff()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/staffs")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Pham Van D"));
    }

    @Test
    @DisplayName("GET /api/staffs without ADMIN role should return 403")
    void testGetAllStaffForbidden() throws Exception {
        mockMvc.perform(get("/api/staffs")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }
}
