package com.rikkeibank.staffservice.controller;

import com.rikkeibank.staffservice.dto.StaffRequest;
import com.rikkeibank.staffservice.entity.Staff;
import com.rikkeibank.staffservice.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/staffs")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<Staff> createStaff(@Valid @RequestBody StaffRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createStaff(request));
    }

    @GetMapping
    public ResponseEntity<List<Staff>> getAllStaff(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Staff> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getStaffById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Staff> updateStaff(@PathVariable Long id, @Valid @RequestBody StaffRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        staffService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    private void validateAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Access denied. ADMIN role required.");
    }
}
