package com.banking.payment.repository;

import com.banking.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(Long sourceAccountId, Long destinationAccountId);
}
