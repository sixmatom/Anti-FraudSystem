package antifraud.response;

import antifraud.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record TransactionResponse(
        Long transactionId,
        Long amount,
        String ip,
        String number,
        String region,
        LocalDateTime date,
        String result,
        String feedback,
        String info
) {
    // Overloaded constructor to handle cases without feedback (e.g., initial transaction evaluation)
    public TransactionResponse(String result, String info) {
        this(null, null, null, null, null, null, result, null, info);
    }

    // Constructor for creating a response from a Transaction entity
    public TransactionResponse(Transaction transaction) {
        this(transaction.getId(), transaction.getAmount(), transaction.getIp(), transaction.getNumber(),
                transaction.getRegion(), transaction.getDate(), transaction.getResult(), transaction.getFeedback() != null ? transaction.getFeedback() : "", null);
    }
}