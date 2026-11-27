package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.ChangePasswordUserCommand;
import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.UpdateUserCommand;
import com.arturoller.medicalclinic.dto.UserDto;
import com.arturoller.medicalclinic.exception.EmailAlreadyTakenException;
import com.arturoller.medicalclinic.exception.InvalidSortFieldException;
import com.arturoller.medicalclinic.exception.UserAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.UserCurrentPasswordDoesNotMatchException;
import com.arturoller.medicalclinic.exception.UserNewPasswordIsTheSameException;
import com.arturoller.medicalclinic.exception.UserNotFoundException;
import com.arturoller.medicalclinic.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @Test
    void list_UsersExist_Returns200AndPage() throws Exception {
        // given
        when(userService.list(PageRequest.of(0, 10))).thenReturn(new PageDto<>(
                List.of(new UserDto(1L, "jan@example.com"),
                        new UserDto(2L, "anna@example.com")),
                0, 10, 2L, 1, false, true));

        // when + then
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].email").value("jan@example.com"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[1].email").value("anna@example.com"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void list_PageSizeAndSortGiven_PassesPageableToService() throws Exception {
        // given
        when(userService.list(any())).thenReturn(new PageDto<>(
                List.of(new UserDto(1L, "jan@example.com")),
                0, 1, 2L, 2, true, false));

        // when + then
        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "email,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("jan@example.com"))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(userService).list(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "email")));
    }

    @Test
    void list_InvalidSortField_Returns400() throws Exception {
        // given
        when(userService.list(PageRequest.of(0, 10, Sort.by("passwordHash"))))
                .thenThrow(new InvalidSortFieldException("Niedozwolone pole sortowania: passwordHash. Dozwolone pola: email, id"));

        // when + then
        mockMvc.perform(get("/users").param("sort", "passwordHash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niedozwolone pole sortowania: passwordHash. Dozwolone pola: email, id"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void getById_UserExists_Returns200AndUser() throws Exception {
        // given
        when(userService.getById(1L)).thenReturn(new UserDto(1L, "jan@example.com"));

        // when + then
        mockMvc.perform(get("/users/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("jan@example.com"));
    }

    @Test
    void getById_UserNotFound_Returns404() throws Exception {
        // given
        when(userService.getById(1L)).thenThrow(new UserNotFoundException("Nie znaleziono użytkownika: 1"));

        // when + then
        mockMvc.perform(get("/users/{userId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono użytkownika: 1"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void update_UserExists_Returns200AndUpdatedUser() throws Exception {
        // given
        UpdateUserCommand command = new UpdateUserCommand("new@example.com");
        when(userService.update(1L, command)).thenReturn(new UserDto(1L, "new@example.com"));

        // when + then
        mockMvc.perform(patch("/users/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void update_UserNotFound_Returns404() throws Exception {
        // given
        UpdateUserCommand command = new UpdateUserCommand("new@example.com");
        when(userService.update(1L, command)).thenThrow(new UserNotFoundException("Nie znaleziono użytkownika: 1"));

        // when + then
        mockMvc.perform(patch("/users/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono użytkownika: 1"));
    }

    @Test
    void update_EmailTaken_Returns409() throws Exception {
        // given
        UpdateUserCommand command = new UpdateUserCommand("taken@example.com");
        when(userService.update(1L, command))
                .thenThrow(new EmailAlreadyTakenException("Adres e-mail jest już zajęty: taken@example.com"));

        // when + then
        mockMvc.perform(patch("/users/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Adres e-mail jest już zajęty: taken@example.com"));
    }

    @Test
    void changePassword_CorrectCurrentPassword_Returns204() throws Exception {
        // given
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("haslo123", "noweHaslo456");

        // when + then
        mockMvc.perform(patch("/users/{userId}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(1L, command);
    }

    @Test
    void changePassword_UserNotFound_Returns404() throws Exception {
        // given
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("haslo123", "noweHaslo456");
        doThrow(new UserNotFoundException("Nie znaleziono użytkownika: 1")).when(userService).changePassword(1L, command);

        // when + then
        mockMvc.perform(patch("/users/{userId}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono użytkownika: 1"));
    }

    @Test
    void changePassword_WrongCurrentPassword_Returns403() throws Exception {
        // given
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("zleHaslo", "noweHaslo456");
        doThrow(new UserCurrentPasswordDoesNotMatchException("Aktualne hasło jest niepoprawne"))
                .when(userService).changePassword(1L, command);

        // when + then
        mockMvc.perform(patch("/users/{userId}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Aktualne hasło jest niepoprawne"))
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));
    }

    @Test
    void changePassword_NewPasswordSameAsCurrent_Returns400() throws Exception {
        // given
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("haslo123", "haslo123");
        doThrow(new UserNewPasswordIsTheSameException("Nowe hasło musi być inne niż aktualne"))
                .when(userService).changePassword(1L, command);

        // when + then
        mockMvc.perform(patch("/users/{userId}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nowe hasło musi być inne niż aktualne"));
    }

    @Test
    void changePassword_MissingNewPassword_Returns400WithoutCallingService() throws Exception {
        // given
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("haslo123", null);

        // when + then
        mockMvc.perform(patch("/users/{userId}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("newPassword: nie może być puste"));

        verifyNoInteractions(userService);
    }

    @Test
    void delete_UserNotAssigned_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/users/{userId}", 1L))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    void delete_UserNotFound_Returns404() throws Exception {
        // given
        doThrow(new UserNotFoundException("Nie znaleziono użytkownika: 1")).when(userService).delete(1L);

        // when + then
        mockMvc.perform(delete("/users/{userId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono użytkownika: 1"));
    }

    @Test
    void delete_UserAssigned_Returns409() throws Exception {
        // given
        doThrow(new UserAlreadyAssignedException("Użytkownik 1 jest powiązany z pacjentem")).when(userService).delete(1L);

        // when + then
        mockMvc.perform(delete("/users/{userId}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Użytkownik 1 jest powiązany z pacjentem"));
    }

    @Test
    void create_EmailFree_Returns201AndUser() throws Exception {
        // given
        CreateUserCommand command = new CreateUserCommand("jan@example.com", "haslo123");
        when(userService.create(command)).thenReturn(new UserDto(1L, "jan@example.com"));

        // when + then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("jan@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void create_InvalidEmail_Returns400WithoutCallingService() throws Exception {
        // given
        CreateUserCommand command = new CreateUserCommand("to-nie-jest-email", "haslo123");

        // when + then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: musi być poprawnym adresem e-mail"));

        verifyNoInteractions(userService);
    }

    @Test
    void create_EmailTaken_Returns409() throws Exception {
        // given
        CreateUserCommand command = new CreateUserCommand("jan@example.com", "haslo123");
        when(userService.create(command))
                .thenThrow(new EmailAlreadyTakenException("Adres e-mail jest już zajęty: jan@example.com"));

        // when + then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Adres e-mail jest już zajęty: jan@example.com"));
    }
}
