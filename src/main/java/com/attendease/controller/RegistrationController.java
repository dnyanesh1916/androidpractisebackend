package com.attendease.controller;

import com.attendease.dto.ApiResponse;
import com.attendease.dto.RegisterRequest;
import com.attendease.dto.UserSummary;
import com.attendease.entity.UserRegistration;
import com.attendease.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "https://androidpractiseui.netlify.app"
})
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserRegistration savedUser = registrationService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Registration successful", savedUser.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/registrations")
    public List<UserSummary> getAllRegistrations() {
        return registrationService.getAllRegistrations().stream()
                .map(registration -> {
                    UserSummary summary = new UserSummary(registration.getId(), registration.getFullName(),
                            registration.getEmail(), "USER", null, false, registration.getCreatedAt());
                    summary.setSource("LEGACY");
                    return summary;
                })
                .toList();
    }
}
