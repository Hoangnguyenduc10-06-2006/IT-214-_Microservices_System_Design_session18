package com.rikkeibank.accountservice.repository;

import com.rikkeibank.accountservice.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
    boolean existsByTypeName(String typeName);
}
