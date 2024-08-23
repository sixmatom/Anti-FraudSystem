package antifraud.service;

import antifraud.entity.FraudLimits;
import antifraud.entity.Transaction;
import antifraud.exception.*;
import antifraud.repository.*;
import antifraud.request.TransactionRequest;
import antifraud.response.TransactionResponse;
import antifraud.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final StolenCardRepository stolenCardRepository;
    private final SuspiciousIPRepository suspiciousIPRepository;
    private final TransactionRepository transactionRepository;
    private final FraudLimitsRepository fraudLimitsRepository;

    @Autowired
    public TransactionService(UserRepository userRepository,
                              StolenCardRepository stolenCardRepository,
                              SuspiciousIPRepository suspiciousIPRepository,
                              TransactionRepository transactionRepository,
                              FraudLimitsRepository fraudLimitsRepository) {
        this.userRepository = userRepository;
        this.stolenCardRepository = stolenCardRepository;
        this.suspiciousIPRepository = suspiciousIPRepository;
        this.transactionRepository = transactionRepository;
        this.fraudLimitsRepository = fraudLimitsRepository;
        initializeLimits();
    }

    private void initializeLimits() {
        if (fraudLimitsRepository.count() == 0) {
            fraudLimitsRepository.findById(1L).orElseGet(() -> {
                FraudLimits newLimits = new FraudLimits();
                newLimits.setMaxAllowed(200L);
                newLimits.setMaxManualProcessing(1500L);
                return fraudLimitsRepository.save(newLimits);
            });
        }
    }

    public TransactionResponse evaluateTransaction(String username, TransactionRequest request) {
        FraudLimits limits = getFraudLimits();
        Long maxAllowed = limits.getMaxAllowed();
        Long maxManualProcessing = limits.getMaxManualProcessing();
        // Validate input
        if (request.amount() <= 0 || request.ip().isEmpty() || request.number().isEmpty() || !isValidRegion(request.region())) {
            throw new IllegalArgumentException("Invalid transaction data");
        }

        // Fetch the user and check if they are locked
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isLocked()) {
            throw new LockedUserException("User is locked");
        }

        // Proceed with transaction evaluation
        Set<String> reasons = new TreeSet<>();

        // Check blacklists
        if (suspiciousIPRepository.existsByIp(request.ip())) {
            reasons.add("ip");
        }
        if (stolenCardRepository.existsByNumber(request.number())) {
            reasons.add("card-number");
        }

        // Check amount thresholds
        if (request.amount() > maxManualProcessing) {
            reasons.add("amount");
        } else if (request.amount() > maxAllowed && reasons.isEmpty()) {
            reasons.add("amount");
        }

        // Check transaction history for correlation rules
        LocalDateTime oneHourAgo = request.date().minusHours(1);
        List<Transaction> recentTransactions = transactionRepository.findAllByNumberAndDateAfter(request.number(), oneHourAgo)
                .stream()
                .filter(transaction -> !transaction.getDate().isAfter(request.date()))
                .toList();

        long regionCount = recentTransactions.stream()
                .map(Transaction::getRegion)
                .filter(region -> !region.equals(request.region()))
                .distinct()
                .count();

        long ipCount = recentTransactions.stream()
                .map(Transaction::getIp)
                .filter(ip -> !ip.equals(request.ip()))
                .distinct()
                .count();

        if (regionCount > 2) {
            reasons.add("region-correlation");
        } else if (regionCount == 2) {
            reasons.add("region-correlation");
        }

        if (ipCount > 2) {
            reasons.add("ip-correlation");
        } else if (ipCount == 2) {
            reasons.add("ip-correlation");
        }

        // Determine the result
        String result;
        if (reasons.isEmpty()) {
            result = "ALLOWED";
        } else if (request.amount() <= maxManualProcessing && (
                (reasons.contains("region-correlation") && regionCount == 2) ||
                        (reasons.contains("ip-correlation") && ipCount == 2) ||
                        (reasons.contains("amount") && reasons.size() == 1))) {
            result = "MANUAL_PROCESSING";
        } else {
            result = "PROHIBITED";
        }

        String info = reasons.isEmpty() ? "none" : String.join(", ", reasons);

        // Save the transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(request.amount());
        transaction.setIp(request.ip());
        transaction.setNumber(request.number());
        transaction.setRegion(request.region());
        transaction.setDate(request.date());
        transaction.setResult(result);
        transaction.setFeedback(null);
        transactionRepository.save(transaction);

        return new TransactionResponse(result, info);
    }


    private FraudLimits getFraudLimits() {
        return fraudLimitsRepository.findById(1L).orElseThrow(() ->
                new EntityNotFoundException("Fraud limits not found"));
    }


    @Transactional
    public TransactionResponse addFeedback(Long transactionId, String feedback) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        validateFeedback(feedback, transaction);

        updateLimitsBasedOnFeedback(feedback, transaction.getResult(), transaction.getAmount());

        transaction.setFeedback(feedback);
        transactionRepository.save(transaction);

        // Return the updated transaction with result included
        return new TransactionResponse(transaction);
    }

    public List<TransactionResponse> getTransactionHistory() {
        return transactionRepository.findAllByOrderByIdAsc().stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionHistoryByNumber(String number) {
        if (!isValidCardNumber(number)) {
            throw new IllegalArgumentException("Invalid card number format");
        }

        List<Transaction> transactions = transactionRepository.findAllByNumberOrderByIdAsc(number);
        if (transactions.isEmpty()) {
            throw new EntityNotFoundException("No transactions found for this card number");
        }

        return transactions.stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    private void validateFeedback(String feedback, Transaction transaction) {
        if (!List.of("ALLOWED", "MANUAL_PROCESSING", "PROHIBITED").contains(feedback)) {
            throw new IllegalArgumentException("Invalid feedback format");
        }
        if (transaction.getFeedback() != null) {
            throw new EntityExistsException("Feedback already exists for this transaction");
        }
        String result = transaction.getResult();
        if (result == null) {
            throw new UnprocessableEntityException("Transaction result is null");
        }

        if ((transaction.getResult().equals("ALLOWED") && feedback.equals("ALLOWED")) ||
                (transaction.getResult().equals("MANUAL_PROCESSING") && feedback.equals("MANUAL_PROCESSING")) ||
                (transaction.getResult().equals("PROHIBITED") && feedback.equals("PROHIBITED"))) {
            throw new UnprocessableEntityException("Invalid feedback according to the table");
        }
    }

    private void updateLimitsBasedOnFeedback(String feedback, String result, Long amount) {
        FraudLimits limits = fraudLimitsRepository.findById(1L).orElseThrow(() ->
                new EntityNotFoundException("Fraud limits not found"));

        Long maxAllowed = limits.getMaxAllowed();
        Long maxManualProcessing = limits.getMaxManualProcessing();

        if (feedback.equals("ALLOWED")) {
            if (result.equals("MANUAL_PROCESSING")) {
                maxAllowed = (long) Math.ceil(0.8 * maxAllowed + 0.2 * amount);
            } else if (result.equals("PROHIBITED")) {
                maxAllowed = (long) Math.ceil(0.8 * maxAllowed + 0.2 * amount);
                maxManualProcessing = (long) Math.ceil(0.8 * maxManualProcessing + 0.2 * amount);
            }
        } else if (feedback.equals("MANUAL_PROCESSING") && result.equals("PROHIBITED")) {
            maxManualProcessing = (long) Math.ceil(0.8 * maxManualProcessing + 0.2 * amount);
        } else if (feedback.equals("MANUAL_PROCESSING") && result.equals("ALLOWED")) {
            maxAllowed = (long) Math.ceil(0.8 * maxAllowed - 0.2 * amount);
        } else if (feedback.equals("PROHIBITED")) {
            if (result.equals("ALLOWED")) {
                maxAllowed = (long) Math.ceil(0.8 * maxAllowed - 0.2 * amount);
                maxManualProcessing = (long) Math.ceil(0.8 * maxManualProcessing - 0.2 * amount);
            } else if (result.equals("MANUAL_PROCESSING")) {
                maxManualProcessing = (long) Math.ceil(0.8 * maxManualProcessing - 0.2 * amount);
            }
        }

        limits.setMaxAllowed(maxAllowed);
        limits.setMaxManualProcessing(maxManualProcessing);
        fraudLimitsRepository.save(limits);
    }

    private boolean isValidCardNumber(String number) {
        // Remove any non-numeric characters
        String cleanedNumber = number.replaceAll("[^0-9]", "");

        // Check if the cleaned number is empty or not numeric
        if (cleanedNumber.isEmpty() || !cleanedNumber.matches("\\d+")) {
            return false;
        }

        // Check the length of the card number
        int length = cleanedNumber.length();
        if (length < 13 || length > 19) {
            return false;
        }

        // Validate using the Luhn algorithm
        return isLuhnValid(cleanedNumber);
    }

    private boolean isLuhnValid(String number) {
        int sum = 0;
        boolean alternate = false;

        // Traverse the number from right to left
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }

            sum += n;
            alternate = !alternate;
        }

        // Valid if the sum is a multiple of 10
        return (sum % 10 == 0);
    }

    private boolean isValidRegion(String region) {
        // Check if region is one of the allowed codes
        return List.of("EAP", "ECA", "HIC", "LAC", "MENA", "SA", "SSA").contains(region);
    }
}
