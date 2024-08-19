package antifraud.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class FraudLimits {

    @Id
    private Long id = 1L; // A single record to store the limits

    private Long maxAllowed;
    private Long maxManualProcessing;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMaxAllowed() {
        return maxAllowed;
    }

    public void setMaxAllowed(Long maxAllowed) {
        this.maxAllowed = maxAllowed;
    }

    public Long getMaxManualProcessing() {
        return maxManualProcessing;
    }

    public void setMaxManualProcessing(Long maxManualProcessing) {
        this.maxManualProcessing = maxManualProcessing;
    }
}
