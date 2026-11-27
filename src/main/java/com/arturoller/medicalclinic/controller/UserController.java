package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.controller.doc.ApiBadRequest;
import com.arturoller.medicalclinic.controller.doc.ApiConflict;
import com.arturoller.medicalclinic.controller.doc.ApiNotFound;
import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Użytkownicy", description = "Konta użytkowników, do których przypisuje się pacjentów i lekarzy")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "Lista użytkowników",
            description = "Zwraca użytkowników zapisanych w systemie. Sortowanie: `id`, `email`.")
    @ApiBadRequest("Niedozwolone pole sortowania")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageDto<UserDto> list(@ParameterObject @PageableDefault Pageable pageable) {
        return userService.list(pageable);
    }

    @Operation(summary = "Pobierz użytkownika po ID")
    @ApiNotFound("Użytkownik nie istnieje")
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto getById(@Parameter(description = "ID użytkownika", example = "1") @PathVariable Long userId) {
        return userService.getById(userId);
    }

    @Operation(summary = "Zmień e-mail użytkownika",
            description = "E-mail musi być poprawny i unikalny.")
    @ApiBadRequest("E-mail jest pusty lub niepoprawny")
    @ApiNotFound("Użytkownik nie istnieje")
    @ApiConflict("E-mail jest zajęty przez innego użytkownika")
    @PatchMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto update(@Parameter(description = "ID użytkownika", example = "1") @PathVariable Long userId,
                          @Valid @RequestBody UpdateUserCommand command) {
        return userService.update(userId, command);
    }

    @Operation(summary = "Zmień hasło",
            description = "Wymaga podania aktualnego hasła. Nowe hasło musi być inne niż obecne.")
    @ApiBadRequest("Brak hasła albo nowe hasło jest takie samo jak obecne")
    @ApiResponse(responseCode = "403", description = "Aktualne hasło jest błędne",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageDto.class),
                    examples = @ExampleObject(value = """
                            {"message": "Aktualne hasło jest niepoprawne", "status": "FORBIDDEN"}""")))
    @ApiNotFound("Użytkownik nie istnieje")
    @PatchMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Parameter(description = "ID użytkownika", example = "1") @PathVariable Long userId,
                               @Valid @RequestBody ChangePasswordUserCommand command) {
        userService.changePassword(userId, command);
    }

    @Operation(summary = "Usuń użytkownika",
            description = "Nie można usunąć użytkownika powiązanego z pacjentem lub lekarzem.")
    @ApiNotFound("Użytkownik nie istnieje")
    @ApiConflict("Użytkownik jest powiązany z pacjentem lub lekarzem")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID użytkownika", example = "1") @PathVariable Long userId) {
        userService.delete(userId);
    }

    @Operation(summary = "Utwórz użytkownika",
            description = "Hasło jest zapisywane jako hash BCrypt i nigdy nie jest zwracane. E-mail musi być unikalny.")
    @ApiBadRequest("Brak pól albo e-mail jest niepoprawny")
    @ApiConflict("E-mail jest już zajęty")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserCommand command) {
        return userService.create(command);
    }
}
