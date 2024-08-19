package antifraud.service;

import antifraud.entity.SuspiciousIP;
import antifraud.exception.EntityExistsException;
import antifraud.exception.EntityNotFoundException;
import antifraud.repository.SuspiciousIPRepository;
import antifraud.request.SuspiciousIPRequest;
import antifraud.response.SuspiciousIPResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SuspiciousIpService {

    private final SuspiciousIPRepository suspiciousIpRepository;

    @Autowired
    public SuspiciousIpService(SuspiciousIPRepository suspiciousIpRepository) {
        this.suspiciousIpRepository = suspiciousIpRepository;
    }

    public static boolean isValidIPv4(String ip) {
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipPattern);
    }

    public SuspiciousIPResponse save(SuspiciousIPRequest request) {
        if (!isValidIPv4(request.ip())) {
            throw new IllegalArgumentException("Invalid IP Address");
        }
        // Check if the IP address is already in the database

        if (suspiciousIpRepository.existsByIp(request.ip())) {
            throw new EntityExistsException("IP address is already in the database");
        }
        SuspiciousIP savedIp = suspiciousIpRepository.save(new SuspiciousIP(request.ip()));
        // Save the new suspicious IP to the database
        return new SuspiciousIPResponse(savedIp.getId(), savedIp.getIp());
    }

    public void deleteByIp(String ip) {
        // Check if the IP address exists in the database
        if (!isValidIPv4(ip)) {
            throw new IllegalArgumentException("Invalid IP Address");
        }
        Optional<SuspiciousIP> suspiciousIp = suspiciousIpRepository.findByIp(ip);
        if (suspiciousIp.isEmpty()) {
            throw new EntityNotFoundException("IP address not found in the database");
        }

        // Delete the suspicious IP from the database
        suspiciousIpRepository.delete(suspiciousIp.get());
    }

    public List<SuspiciousIP> findAll() {
        // Retrieve all suspicious IPs sorted by ID in ascending order
        return suspiciousIpRepository.findAllByOrderByIdAsc();
    }

    public boolean isIpBlacklisted(String ip) {
        // Implement IP blacklist check
        return suspiciousIpRepository.existsByIp(ip);
    }


}

