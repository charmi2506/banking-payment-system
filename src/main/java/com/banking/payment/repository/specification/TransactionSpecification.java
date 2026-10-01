package com.banking.payment.repository.specification;

import com.banking.payment.entity.Account;
import com.banking.payment.entity.Transaction;
import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<Transaction> filterTransactions(
            List<Account> userAccounts,
            Account specificAccount,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Account ownership filter: user only sees transactions where their account is source or destination
            if (specificAccount != null) {
                Predicate sourcePredicate = criteriaBuilder.equal(root.get("sourceAccount"), specificAccount);
                Predicate destPredicate = criteriaBuilder.equal(root.get("destinationAccount"), specificAccount);
                predicates.add(criteriaBuilder.or(sourcePredicate, destPredicate));
            } else if (userAccounts != null && !userAccounts.isEmpty()) {
                Predicate sourcePredicate = root.get("sourceAccount").in(userAccounts);
                Predicate destPredicate = root.get("destinationAccount").in(userAccounts);
                predicates.add(criteriaBuilder.or(sourcePredicate, destPredicate));
            } else {
                // If user owns no accounts, no transactions should be returned
                return criteriaBuilder.disjunction();
            }

            // 2. Transaction Type filter
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            // 3. Transaction Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // 4. Start Date filter
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            // 5. End Date filter
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
