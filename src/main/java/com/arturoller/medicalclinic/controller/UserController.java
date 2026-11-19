package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<UserDto> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{userId}")
    public UserDto getById(@PathVariable Integer userId) {
        return userService.getById(userId);
    }

    @PatchMapping("/{userId}")
    public UserDto update(@PathVariable Integer userId, @RequestBody UpdateUserCommand command) {
        return userService.update(userId, command);
    }

    @PatchMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable Integer userId, @RequestBody ChangePasswordUserCommand command) {
        userService.changePassword(userId, command);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer userId) {
        userService.delete(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody CreateUserCommand command) {
        return userService.create(command);
    }
}
