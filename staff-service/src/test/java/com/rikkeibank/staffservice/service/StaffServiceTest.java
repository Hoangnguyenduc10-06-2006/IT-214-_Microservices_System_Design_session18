package com.rikkeibank.staffservice.service;

import com.rikkeibank.staffservice.dto.StaffRequest;
import com.rikkeibank.staffservice.entity.Staff;
import com.rikkeibank.staffservice.entity.StaffStatus;
import com.rikkeibank.staffservice.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StaffService staffService;

    private Staff sampleStaff;

    @BeforeEach
    void setUp() {
        sampleStaff = Staff.builder()
                .id(1L)
                .fullName("Le Thi C")
                .email("thic@rikkeibank.com")
                .phone("0933445566")
                .position("TELLER")
                .department("Transaction Operations")
                .status(StaffStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Create Staff: Should save and return new staff")
    void testCreateStaff() {
        StaffRequest request = StaffRequest.builder()
                .fullName("Le Thi C")
                .email("thic@rikkeibank.com")
                .phone("0933445566")
                .position("TELLER")
                .department("Transaction Operations")
                .build();

        when(staffRepository.save(any(Staff.class))).thenReturn(sampleStaff);

        Staff created = staffService.createStaff(request);

        assertNotNull(created);
        assertEquals("Le Thi C", created.getFullName());
        assertEquals("TELLER", created.getPosition());
    }

    @Test
    @DisplayName("Get Staff By ID: Should return staff when found")
    void testGetStaffById() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(sampleStaff));

        Staff result = staffService.getStaffById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Get All Staff: Should return staff list")
    void testGetAllStaff() {
        when(staffRepository.findAll()).thenReturn(List.of(sampleStaff));

        List<Staff> list = staffService.getAllStaff();

        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Delete Staff: Should delete when exists")
    void testDeleteStaff() {
        when(staffRepository.existsById(1L)).thenReturn(true);
        doNothing().when(staffRepository).deleteById(1L);

        assertDoesNotThrow(() -> staffService.deleteStaff(1L));
        verify(staffRepository, times(1)).deleteById(1L);
    }
}
