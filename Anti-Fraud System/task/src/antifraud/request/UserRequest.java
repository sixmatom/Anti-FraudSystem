package antifraud.request;


import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank(message = "Name is mandatory") String name,
        @NotBlank(message = "Password is mandatory") String password,
        @NotBlank(message = "Username is mandatory") String username
) {
}