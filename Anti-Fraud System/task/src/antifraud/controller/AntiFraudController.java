package antifraud.controller;

import antifraud.entity.StolenCard;
import antifraud.entity.SuspiciousIP;
import antifraud.request.FeedbackRequest;
import antifraud.request.StolenCardRequest;
import antifraud.request.SuspiciousIPRequest;
import antifraud.request.TransactionRequest;
import antifraud.response.DeleteIpResponse;
import antifraud.response.StolenCardResponse;
import antifraud.response.SuspiciousIPResponse;
import antifraud.response.TransactionResponse;
import antifraud.service.StolenCardService;
import antifraud.service.SuspiciousIpService;
import antifraud.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/antifraud")
public class AntiFraudController {

    private final TransactionService transactionService;

    @Autowired
    private SuspiciousIpService suspiciousIpService;
    @Autowired
    private StolenCardService stolenCardService;

    @Autowired
    public AntiFraudController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transaction")

    public TransactionResponse evaluateTransaction(@RequestBody TransactionRequest request) {
        // Get the current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Retrieve the username from the authentication object
        String username = authentication != null ? authentication.getName() : null;

        // Use the username and amount from the request
        return transactionService.evaluateTransaction(username, request);
    }

    @PostMapping("/suspicious-ip")
    public SuspiciousIPResponse suspiciousIp(@RequestBody SuspiciousIPRequest request) {

        return suspiciousIpService.save(request);
    }

    @DeleteMapping("/suspicious-ip/{ip}")
    public DeleteIpResponse deleteSuspiciousIP(@PathVariable String ip) {
        suspiciousIpService.deleteByIp(ip);
        return new DeleteIpResponse("IP " + ip + " successfully removed!");
    }

    @GetMapping("/suspicious-ip")
    public ResponseEntity<List<SuspiciousIP>> getAllSuspiciousIPs() {
        return ResponseEntity.ok(suspiciousIpService.findAll());
    }

    @PostMapping("/stolencard")
    public StolenCardResponse addStolenCard(@RequestBody StolenCardRequest stolenCard) {
        if (!StolenCardService.isValidCardNumber(stolenCard.number())) {
            throw new IllegalArgumentException("Invalid card number");
        }
        return stolenCardService.save(stolenCard.number());
    }

    @DeleteMapping("/stolencard/{number}")
    public DeleteIpResponse deleteStolenCard(@PathVariable String number) {
        if (!StolenCardService.isValidCardNumber(number)) {
            throw new IllegalArgumentException("invalid card number");
        }
        stolenCardService.deleteByNumber(number);
        return new DeleteIpResponse("Card " + number + " successfully removed!");
    }

    @GetMapping("/stolencard")
    public ResponseEntity<List<StolenCard>> getAllStolenCards() {
        return ResponseEntity.ok(stolenCardService.findAll());
    }

    @PutMapping("/transaction")
    public ResponseEntity<TransactionResponse> addFeedback(@RequestBody FeedbackRequest feedbackRequest) {
        TransactionResponse response = transactionService.addFeedback(feedbackRequest.transactionId(), feedbackRequest.feedback());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory() {
        List<TransactionResponse> history = transactionService.getTransactionHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/{number}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistoryByNumber(@PathVariable String number) {
        List<TransactionResponse> history = transactionService.getTransactionHistoryByNumber(number);
        return ResponseEntity.ok(history);
    }
}


