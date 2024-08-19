package antifraud.response;

public record ErrorResponse(String error) {
    public ErrorResponse(Exception e) {
        this(e.getMessage());
    }


}
