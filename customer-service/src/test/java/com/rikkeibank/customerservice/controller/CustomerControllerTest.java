package com.rikkeibank.customerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.customerservice.dto.CustomerRequest;
import com.rikkeibank.customerservice.entity.Customer;
import com.rikkeibank.customerservice.entity.CustomerStatus;
import com.rikkeibank.customerservice.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("GET /api/customers by ADMIN should return 200 with list")
    void testGetAllCustomersByAdmin() throws Exception {
        Customer c = Customer.builder()
                .id(1L)
                .fullName("Tran Van B")
                .email("vanb@rikkei.vn")
                .phone("0981112223")
                .identityNumber("001122334455")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerService.getAllCustomers()).thenReturn(List.of(c));

        mockMvc.perform(get("/api/customers")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Tran Van B"));
    }

    @Test
    @DisplayName("GET /api/customers without ADMIN role should return 403 Forbidden")
    void testGetAllCustomersForbidden() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/customers/{id} should return customer details")
    void testGetCustomerById() throws Exception {
        Customer c = Customer.builder()
                .id(1L)
                .fullName("Tran Van B")
                .email("vanb@rikkei.vn")
                .phone("0981112223")
                .identityNumber("001122334455")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerService.getCustomerById(1L)).thenReturn(c);

        mockMvc.perform(get("/api/customers/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
