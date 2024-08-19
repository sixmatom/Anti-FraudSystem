package antifraud.service;

import antifraud.exception.LockedUserException;
import antifraud.exception.UserAlreadyExistsException;
import antifraud.exception.UserNotFoundException;
import antifraud.repository.UserRepository;
import antifraud.request.UserRequest;
import antifraud.response.ChangeRoleResponse;
import antifraud.response.UserResponse;
import antifraud.user.Role;
import antifraud.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse registerUser(UserRequest userRequest) {
        if (userRepository.existsByUsername(userRequest.username())) {
            throw new UserAlreadyExistsException("User already exists");
        }

        Role role = userRepository.count() == 0 ? Role.ADMINISTRATOR : Role.MERCHANT;
        User user = new User();
        user.setName(userRequest.name());
        user.setUsername(userRequest.username());
        user.setPassword(passwordEncoder.encode(userRequest.password()));
        user.setRole(role);

        user.setLocked(role != Role.ADMINISTRATOR);

        user = userRepository.save(user);

        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole().name());
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole().name()))
                .sorted(Comparator.comparing(UserResponse::id))
                .collect(Collectors.toList());
    }

    public void checkIfUserIsLocked(User user) {
        if (user.isLocked()) {
            throw new LockedUserException("User is locked and cannot make transactions");
        }
    }

    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        userRepository.delete(user);
    }

    // New method to change user role
    public ChangeRoleResponse changeUserRole(String username, String newRole) {
        if (!newRole.equals("MERCHANT") && !newRole.equals("SUPPORT")) {
            throw new IllegalArgumentException("Invalid role");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Role role = Role.valueOf(newRole.toUpperCase());

        if (user.getRole().equals(role)) {
            throw new UserAlreadyExistsException("User already has this role");
        }

        user.setRole(role);
        userRepository.save(user);

        return new ChangeRoleResponse(user.getId(), user.getName(), user.getUsername(), user.getRole().name());
    }

    // New method to lock/unlock user
    public void changeUserAccess(String username, String operation) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() == Role.ADMINISTRATOR) {
            throw new IllegalArgumentException("Cannot lock/unlock an ADMINISTRATOR");
        }

        if ("LOCK".equalsIgnoreCase(operation)) {
            user.setLocked(true);
        } else if ("UNLOCK".equalsIgnoreCase(operation)) {
            user.setLocked(false);
        } else {
            throw new IllegalArgumentException("Invalid operation: " + operation);
        }

        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getAuthorities()
        );
    }


}
