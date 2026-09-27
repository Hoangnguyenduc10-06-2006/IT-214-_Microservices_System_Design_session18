package com.rikkeibank.staffservice.service;

import com.rikkeibank.staffservice.dto.StaffRequest;
import com.rikkeibank.staffservice.entity.Staff;
import com.rikkeibank.staffservice.entity.StaffStatus;
import com.rikkeibank.staffservice.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    public Staff createStaff(StaffRequest request) {
        Staff staff = Staff.builder()
                .fullName(request.getFullName()).email(request.getEmail())
                .phone(request.getPhone()).position(request.getPosition())
                .department(request.getDepartment()).status(StaffStatus.ACTIVE).build();
        return staffRepository.save(staff);
    }

    public Staff getStaffById(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + id));
    }

    public List<Staff> getAllStaff() { return staffRepository.findAll(); }

    public Staff updateStaff(Long id, StaffRequest request) {
        Staff staff = getStaffById(id);
        staff.setFullName(request.getFullName()); staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone()); staff.setPosition(request.getPosition());
        staff.setDepartment(request.getDepartment());
        return staffRepository.save(staff);
    }

    public void deleteStaff(Long id) {
        if (!staffRepository.existsById(id)) throw new RuntimeException("Staff not found with id: " + id);
        staffRepository.deleteById(id);
    }
}
