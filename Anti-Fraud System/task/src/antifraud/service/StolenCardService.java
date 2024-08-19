package antifraud.service;


import antifraud.entity.StolenCard;
import antifraud.exception.EntityExistsException;
import antifraud.exception.EntityNotFoundException;
import antifraud.repository.StolenCardRepository;
import antifraud.response.StolenCardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StolenCardService {

    private final StolenCardRepository stolenCardRepository;

    @Autowired
    public StolenCardService(StolenCardRepository stolenCardRepository) {
        this.stolenCardRepository = stolenCardRepository;
    }

    public static boolean isValidCardNumber(String number) {
        int nDigits = number.length();
        int sum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--) {
            int d = number.charAt(i) - '0';
            if (isSecond) d *= 2;
            sum += d / 10;
            sum += d % 10;
            isSecond = !isSecond;
        }
        return (sum % 10 == 0);
    }

    public boolean isCardNumberBlacklisted(String number) {
        // Implement card number blacklist check

        return stolenCardRepository.existsByNumber(number);
    }
    // Save the new stolen card to the database

    public StolenCardResponse save(String cardNumber) {
        // Check if the card number is already in the database
        Optional<StolenCard> existingCard = stolenCardRepository.findByNumber(cardNumber);
        if (existingCard.isPresent()) {
            throw new EntityExistsException("Card number is already in the database");
        }

        StolenCard stolenCard = new StolenCard();
        stolenCard.setNumber(cardNumber);
        stolenCardRepository.save(stolenCard);
        return new StolenCardResponse(stolenCard.getId(), stolenCard.getNumber());
    }

    public void deleteByNumber(String number) {
        // Check if the card number exists in the database
        Optional<StolenCard> stolenCard = stolenCardRepository.findByNumber(number);
        if (stolenCard.isEmpty()) {
            throw new EntityNotFoundException("Card number not found in the database");
        }

        // Delete the stolen card from the database
        stolenCardRepository.delete(stolenCard.get());
    }

    public List<StolenCard> findAll() {
        // Retrieve all stolen cards sorted by ID in ascending order
        return stolenCardRepository.findAllByOrderByIdAsc();
    }
}

