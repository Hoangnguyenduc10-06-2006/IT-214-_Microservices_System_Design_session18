package com.rikkeibank.customerservice.service;

import com.rikkeibank.customerservice.dto.CustomerRequest;
import com.rikkeibank.customerservice.entity.Customer;
import com.rikkeibank.customerservice.entity.CustomerStatus;
import com.rikkeibank.customerservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Customer service with Redis Cache-Aside pattern.
 * @Cacheable - Read from cache first, populate on miss
 * @CachePut  - Update cache on write
 * @CacheEvict - Invalidate cache on delete
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer createCustomer(CustomerRequest request) {
        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .identityNumber(request.getIdentityNumber())
                .status(CustomerStatus.ACTIVE)
                .build();
        return customerRepository.save(customer);
    }

    @Cacheable(value = "customers", key = "#id")
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @CachePut(value = "customers", key = "#id")
    public Customer updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setIdentityNumber(request.getIdentityNumber());

        return customerRepository.save(customer);
    }

    @CacheEvict(value = "customers", key = "#id")
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
    }
}
