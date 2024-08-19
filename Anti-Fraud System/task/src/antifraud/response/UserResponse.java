package antifraud.response;


public record UserResponse(
        Long id,
        String name,
        String username,
        String role) {
}

