package antifraud.request;

import jakarta.validation.constraints.NotBlank;

public record SuspiciousIPRequest(@NotBlank String ip) {
}
