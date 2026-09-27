package com.banking.payment.service.kafka;

import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.AuditLog;
import com.banking.payment.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventListener {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "${app.kafka.topics.transaction-events}", groupId = "banking-service-group")
    @Transactional
    public void handleTransactionEvent(TransferResponse transfer) {
        log.info("Received Kafka transaction event for reference: {}", transfer.getReferenceNumber());
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action("FUND_TRANSFER_COMPLETED")
                    .performedBy("SYSTEM_KAFKA_LISTENER")
                    .entityName("Transaction")
                    .entityId(transfer.getReferenceNumber())
                    .details("Transferred " + transfer.getAmount() + " from " +
                             transfer.getSourceAccountNumber() + " to " + transfer.getDestinationAccountNumber())
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Successfully recorded audit log in PostgreSQL for reference: {}", transfer.getReferenceNumber());
        } catch (Exception e) {
            log.error("Failed to process Kafka transaction event: {}", e.getMessage(), e);
        }
    }
}
