package antifraud.request;

import java.time.LocalDateTime;

public record TransactionRequest(long amount, String ip, String number, String region, LocalDateTime date) {
}
