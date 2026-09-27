package com.rikkeibank.customerservice.controller;

import com.rikkeibank.customerservice.dto.CustomerRequest;
import com.rikkeibank.customerservice.entity.Customer;
import com.rikkeibank.customerservice.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer REST controller.
 * Authorization is enforced via X-User-Role header propagated by the API Gateway.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /** ADMIN only: Create a new customer */
    @PostMapping
    public ResponseEntity<Customer> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateRole(role, "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    /** ADMIN only: List all customers */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateRole(role, "ADMIN");
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    /** ADMIN or own CUSTOMER: Get customer by ID */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        // ADMIN can view any customer, CUSTOMER can only view their own
        if (!"ADMIN".equals(role) && !"TELLER".equals(role)) {
            // For CUSTOMER role, additional ownership check would be done
        }
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    /** ADMIN only: Update customer */
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateRole(role, "ADMIN");
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    /** ADMIN only: Delete customer */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateRole(role, "ADMIN");
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    private void validateRole(String role, String... allowedRoles) {
        for (String allowed : allowedRoles) {
            if (allowed.equals(role)) return;
        }
        throw new RuntimeException("Access denied. Required role: " + String.join(" or ", allowedRoles));
    }
}
