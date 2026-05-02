package com.trojanmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trojanmarket.dto.AuthResponseDTO;
import com.trojanmarket.dto.LoginRequest;
import com.trojanmarket.dto.SignupRequest;
import com.trojanmarket.dto.VerifyEmailRequest;
import com.trojanmarket.repository.UserRepository;
import com.trojanmarket.security.ForbiddenException;
import com.trojanmarket.security.JwtFilter;
import com.trojanmarket.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link AuthController}.
 *
 * Verifies the HTTP shape of signup → verify-email → login: validation rejects
 * malformed bodies, the controller delegates to {@link AuthService}, and the
 * verify/login responses propagate the JWT payload back to the client.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class))
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockBean AuthService authService;
    @MockBean UserRepository userRepository;

    @Test
    void signupAcceptsValidPayload() throws Exception {
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SignupRequest(
                                "Tommy", "Trojan", "ttrojan@usc.edu", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("verification_required"))
                .andExpect(jsonPath("$.email").value("ttrojan@usc.edu"));
    }

    @Test
    void signupRejectsBlankFields() throws Exception {
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SignupRequest(
                                "", "Trojan", "ttrojan@usc.edu", "Password1!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupRejectsShortPassword() throws Exception {
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SignupRequest(
                                "Tommy", "Trojan", "ttrojan@usc.edu", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyReturnsJwtAndUserPayload() throws Exception {
        when(authService.verifyEmail(any(VerifyEmailRequest.class))).thenReturn(
                AuthResponseDTO.builder()
                        .token("JWT").userID(42).username("ttrojan")
                        .firstName("Tommy").lastName("Trojan")
                        .email("ttrojan@usc.edu").isVerified(true).build());

        mvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new VerifyEmailRequest(
                                "ttrojan@usc.edu", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("JWT"))
                .andExpect(jsonPath("$.firstName").value("Tommy"))
                .andExpect(jsonPath("$.isVerified").value(true));
    }

    @Test
    void loginReturnsJwt() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                AuthResponseDTO.builder()
                        .token("JWT").userID(42).username("ttrojan")
                        .firstName("Tommy").lastName("Trojan")
                        .email("ttrojan@usc.edu").isVerified(true).build());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest(
                                "ttrojan@usc.edu", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("JWT"))
                .andExpect(jsonPath("$.email").value("ttrojan@usc.edu"));
    }

    @Test
    @WithMockUser
    void loginUnverifiedReturns403() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ForbiddenException("Please verify your email before logging in"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest(
                                "ttrojan@usc.edu", "Password1!"))))
                .andExpect(status().isForbidden());
    }
}
