package antifraud.controller;


import antifraud.request.AccessChangeRequest;
import antifraud.request.RoleChangeRequest;
import antifraud.request.UserRequest;
import antifraud.response.AccessChangeResponse;
import antifraud.response.ChangeRoleResponse;
import antifraud.response.DeletionResponse;
import antifraud.response.UserResponse;
import antifraud.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/user")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@RequestBody @Valid UserRequest userRequest) {

        // Attempt to register the user
        UserResponse userResponse = userService.registerUser(userRequest);
        return new UserResponse(userResponse.id(), userResponse.name(), userResponse.username(), userResponse.role());


    }

    @GetMapping("/list")
    public ResponseEntity<List<UserResponse>> listUsers() {
        List<UserResponse> users = userService.listUsers();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/user/{username}")
    public DeletionResponse deleteUser(@PathVariable String username) {

        userService.deleteUser(username);
        return (new DeletionResponse(username, "Deleted successfully!"));

    }

    @PutMapping("/role")
    public ChangeRoleResponse changeUserRole(@RequestBody RoleChangeRequest request) {


        // Return the updated user details
        return userService.changeUserRole(request.username(), request.role());
    }

    @PutMapping("/access")
    public AccessChangeResponse changeUserAccess(@RequestBody AccessChangeRequest request) {
        userService.changeUserAccess(request.username(), request.operation());
        String operation = "locked";
        if (request.operation().equals("UNLOCK")) {
            operation = "unlocked";
        }
        String statusMessage = String.format("User %s %s!", request.username(), operation);
        return (new AccessChangeResponse(statusMessage));
    }
}
