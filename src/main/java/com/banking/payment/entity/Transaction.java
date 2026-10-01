package com.banking.payment.entity;

import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_source_account", columnList = "source_account_id"),
        @Index(name = "idx_txn_dest_account", columnList = "destination_account_id"),
        @Index(name = "idx_txn_created_at", columnList = "created_at"),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_type", columnList = "type"),
        @Index(name = "idx_txn_idempotency_key", columnList = "idempotency_key"),
        @Index(name = "idx_txn_src_created", columnList = "source_account_id, created_at"),
        @Index(name = "idx_txn_dst_created", columnList = "destination_account_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 50)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(length = 255)
    private String description;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
