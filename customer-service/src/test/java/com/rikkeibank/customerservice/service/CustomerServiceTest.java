package com.rikkeibank.customerservice.service;

import com.rikkeibank.customerservice.dto.CustomerRequest;
import com.rikkeibank.customerservice.entity.Customer;
import com.rikkeibank.customerservice.entity.CustomerStatus;
import com.rikkeibank.customerservice.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("vana@rikkeibank.com")
                .phone("0912345678")
                .address("Hanoi, Vietnam")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .identityNumber("001234567890")
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Create Customer: Should save and return new customer")
    void testCreateCustomer() {
        CustomerRequest request = CustomerRequest.builder()
                .fullName("Nguyen Van A")
                .email("vana@rikkeibank.com")
                .phone("0912345678")
                .address("Hanoi, Vietnam")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .identityNumber("001234567890")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        Customer created = customerService.createCustomer(request);

        assertNotNull(created);
        assertEquals("Nguyen Van A", created.getFullName());
        assertEquals(CustomerStatus.ACTIVE, created.getStatus());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Get Customer By ID: Should return customer when found")
    void testGetCustomerByIdSuccess() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        Customer result = customerService.getCustomerById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Nguyen Van A", result.getFullName());
    }

    @Test
    @DisplayName("Get Customer By ID: Should throw exception when not found")
    void testGetCustomerByIdNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> customerService.getCustomerById(99L));
        assertTrue(ex.getMessage().contains("Customer not found"));
    }

    @Test
    @DisplayName("Get All Customers: Should return customer list")
    void testGetAllCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(sampleCustomer));

        List<Customer> list = customerService.getAllCustomers();

        assertEquals(1, list.size());
        assertEquals("Nguyen Van A", list.get(0).getFullName());
    }

    @Test
    @DisplayName("Update Customer: Should update details and save")
    void testUpdateCustomer() {
        CustomerRequest updateReq = CustomerRequest.builder()
                .fullName("Nguyen Van A Updated")
                .email("updated@rikkeibank.com")
                .phone("0987654321")
                .address("HCM City")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .identityNumber("001234567890")
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        Customer updated = customerService.updateCustomer(1L, updateReq);

        assertNotNull(updated);
        verify(customerRepository).save(sampleCustomer);
    }

    @Test
    @DisplayName("Delete Customer: Should delete when exists")
    void testDeleteCustomer() {
        when(customerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(customerRepository).deleteById(1L);

        assertDoesNotThrow(() -> customerService.deleteCustomer(1L));
        verify(customerRepository, times(1)).deleteById(1L);
    }
}
