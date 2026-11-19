package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.ChangePasswordUserCommand;
import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.UpdateUserCommand;
import com.arturoller.medicalclinic.dto.UserDto;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.UserMapper;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.DoctorRepository;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getById(Integer userId) {
        return userMapper.toDto(findById(userId));
    }

    @Transactional
    public UserDto create(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyTakenException("Email arleady exists: " + command.email(), HttpStatus.CONFLICT);
        }
        User user = userRepository.save(userMapper.toModel(command));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto update(Integer userId, UpdateUserCommand command) {
        User user = findById(userId);
        if (!user.getEmail().equals(command.email()) && userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyTakenException("Email arleady exists: " + command.email(), HttpStatus.CONFLICT);
        }
        user.update(command);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Integer userId, ChangePasswordUserCommand command) {
        User user = findById(userId);
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new UserCurrentPasswordDoesNotMatch("Current password doesn't match old password!", HttpStatus.FORBIDDEN);
        }
        if (passwordEncoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new UserCurrentPasswordIsTheSame("New password is the same! Change it!", HttpStatus.FORBIDDEN);
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void delete(Integer userId) {
        User user = findById(userId);
        if (patientRepository.existsByUserId(userId)) {
            throw new UserAlreadyAssignedException("User is assigned to a patient: " + userId, HttpStatus.CONFLICT);
        }
        if (doctorRepository.existsByUserId(userId)) {
            throw new UserAlreadyAssignedException("User is assigned to a doctor: " + userId, HttpStatus.CONFLICT);
        }
        userRepository.delete(user);
    }

    private User findById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId, HttpStatus.NOT_FOUND));
    }
}
