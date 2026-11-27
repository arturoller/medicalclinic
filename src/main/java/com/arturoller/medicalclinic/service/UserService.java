package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.UserMapper;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.DoctorRepository;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.UserRepository;
import com.arturoller.medicalclinic.util.SortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "email");
    private final SortValidator sortValidator;

    @Transactional(readOnly = true)
    public PageDto<UserDto> list(Pageable pageable) {
        sortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);
        Page<UserDto> content = userRepository.findAll(pageable)
                .map(userMapper::toDto);
        return PageDto.from(content);
    }

    @Transactional(readOnly = true)
    public UserDto getById(Long userId) {
        return userMapper.toDto(findById(userId));
    }

    @Transactional
    public UserDto create(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyTakenException("Adres e-mail jest już zajęty: " + command.email());
        }
        User user = userRepository.save(userMapper.toModel(command));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto update(Long userId, UpdateUserCommand command) {
        User user = findById(userId);
        if (!user.getEmail().equals(command.email()) && userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyTakenException("Adres e-mail jest już zajęty: " + command.email());
        }
        user.update(command);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordUserCommand command) {
        User user = findById(userId);
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new UserCurrentPasswordDoesNotMatchException("Aktualne hasło jest niepoprawne");
        }
        if (passwordEncoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new UserNewPasswordIsTheSameException("Nowe hasło musi być inne niż aktualne");
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long userId) {
        User user = findById(userId);
        if (patientRepository.existsByUserId(userId)) {
            throw new UserAlreadyAssignedException("Użytkownik " + userId + " jest powiązany z pacjentem");
        }
        if (doctorRepository.existsByUserId(userId)) {
            throw new UserAlreadyAssignedException("Użytkownik " + userId + " jest powiązany z lekarzem");
        }
        userRepository.delete(user);
    }

    private User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Nie znaleziono użytkownika: " + userId));
    }
}
