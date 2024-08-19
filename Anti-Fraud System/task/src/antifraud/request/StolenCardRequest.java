package antifraud.request;

import jakarta.validation.constraints.NotBlank;

public record StolenCardRequest(@NotBlank String number) {
}
