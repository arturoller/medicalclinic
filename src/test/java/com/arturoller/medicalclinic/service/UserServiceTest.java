package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.config.SecurityConfig;
import com.arturoller.medicalclinic.dto.ChangePasswordUserCommand;
import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.UpdateUserCommand;
import com.arturoller.medicalclinic.dto.UserDto;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.UserMapper;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.DoctorRepository;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.UserRepository;
import com.arturoller.medicalclinic.util.SortValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private UserRepository userRepository;
    private PatientRepository patientRepository;
    private DoctorRepository doctorRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        patientRepository = mock(PatientRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        passwordEncoder = new SecurityConfig().passwordEncoder();
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        ReflectionTestUtils.setField(userMapper, "passwordEncoder", passwordEncoder);
        userService = new UserService(userRepository, patientRepository, doctorRepository, userMapper, passwordEncoder,
                new SortValidator());
    }

    @Test
    void list_UsersExist_ReturnsPageOfUserDtos() {
        // given
        Pageable pageable = PageRequest.of(1, 2);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                user(3L, "jan@example.com", "hash"),
                user(4L, "anna@example.com", "hash")), pageable, 4));

        // when
        PageDto<UserDto> result = userService.list(pageable);

        // then
        assertAll(
                () -> assertEquals(2, result.content().size()),
                () -> assertEquals(3L, result.content().get(0).id()),
                () -> assertEquals("anna@example.com", result.content().get(1).email()),
                () -> assertEquals(1, result.pageNumber()),
                () -> assertEquals(2, result.pageSize()),
                () -> assertEquals(4L, result.totalElements()),
                () -> assertEquals(2, result.totalPages()),
                () -> assertFalse(result.hasNext()),
                () -> assertTrue(result.isLast())
        );
    }

    @Test
    void list_InvalidSortField_ThrowsInvalidSortFieldException() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("passwordHash"));

        // when
        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class,
                () -> userService.list(pageable));

        // then
        // kolejność dozwolonych pól pochodzi z Set.of(), więc sprawdzamy tylko początek komunikatu
        assertTrue(exception.getMessage().startsWith("Niedozwolone pole sortowania: passwordHash. Dozwolone pola: "),
                exception.getMessage());
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void list_ValidSortField_PassesSortToRepository() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "email"));
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                user(1L, "jan@example.com", "hash")), pageable, 1));

        // when
        PageDto<UserDto> result = userService.list(pageable);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(captor.capture());
        assertAll(
                () -> assertEquals(Sort.by(Sort.Direction.DESC, "email"), captor.getValue().getSort()),
                () -> assertEquals(1, result.content().size()),
                () -> assertEquals("jan@example.com", result.content().get(0).email())
        );
    }

    @Test
    void getById_UserExists_ReturnsUserDto() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "jan@example.com", "hash")));

        // when
        UserDto result = userService.getById(1L);

        // then
        assertAll(
                () -> assertEquals(1L, result.id()),
                () -> assertEquals("jan@example.com", result.email())
        );
    }

    @Test
    void getById_UserNotFound_ThrowsUserNotFoundException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.getById(1L));

        // then
        assertEquals("Nie znaleziono użytkownika: 1", exception.getMessage());
    }

    @Test
    void create_EmailFree_SavesUserWithHashedPassword() {
        // given
        CreateUserCommand command = new CreateUserCommand("jan@example.com", "haslo123");
        when(userRepository.existsByEmail("jan@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserDto result = userService.create(command);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertAll(
                () -> assertEquals("jan@example.com", result.email()),
                () -> assertNotEquals("haslo123", captor.getValue().getPasswordHash()),
                () -> assertTrue(passwordEncoder.matches("haslo123", captor.getValue().getPasswordHash()))
        );
    }

    @Test
    void create_EmailTaken_ThrowsEmailAlreadyTakenException() {
        // given
        CreateUserCommand command = new CreateUserCommand("jan@example.com", "haslo123");
        when(userRepository.existsByEmail("jan@example.com")).thenReturn(true);

        // when
        EmailAlreadyTakenException exception = assertThrows(EmailAlreadyTakenException.class, () -> userService.create(command));

        // then
        assertEquals("Adres e-mail jest już zajęty: jan@example.com", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_NewEmailFree_UpdatesEmail() {
        // given
        User user = user(1L, "old@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        // when
        UserDto result = userService.update(1L, new UpdateUserCommand("new@example.com"));

        // then
        assertEquals("new@example.com", result.email());
        verify(userRepository).save(user);
    }

    @Test
    void update_SameEmail_DoesNotCheckEmailAvailability() {
        // given
        User user = user(1L, "old@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // when
        UserDto result = userService.update(1L, new UpdateUserCommand("old@example.com"));

        // then
        assertEquals("old@example.com", result.email());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void update_NewEmailTaken_ThrowsEmailAlreadyTakenException() {
        // given
        User user = user(1L, "old@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        UpdateUserCommand command = new UpdateUserCommand("taken@example.com");

        // when
        EmailAlreadyTakenException exception = assertThrows(EmailAlreadyTakenException.class, () -> userService.update(1L, command));

        // then
        assertEquals("Adres e-mail jest już zajęty: taken@example.com", exception.getMessage());
        assertEquals("old@example.com", user.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_UserNotFound_ThrowsUserNotFoundException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        UpdateUserCommand command = new UpdateUserCommand("new@example.com");

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.update(1L, command));

        // then
        assertEquals("Nie znaleziono użytkownika: 1", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_CorrectCurrentPassword_SavesNewHash() {
        // given
        User user = user(1L, "jan@example.com", passwordEncoder.encode("haslo123"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        userService.changePassword(1L, new ChangePasswordUserCommand("haslo123", "noweHaslo456"));

        // then
        assertTrue(passwordEncoder.matches("noweHaslo456", user.getPasswordHash()));
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_UserNotFound_ThrowsUserNotFoundException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("haslo123", "noweHaslo456");

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.changePassword(1L, command));

        // then
        assertEquals("Nie znaleziono użytkownika: 1", exception.getMessage());
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsUserCurrentPasswordDoesNotMatchException() {
        // given
        User user = user(1L, "jan@example.com", passwordEncoder.encode("haslo123"));
        String oldHash = user.getPasswordHash();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("zleHaslo", "noweHaslo456");

        // when
        UserCurrentPasswordDoesNotMatchException exception = assertThrows(UserCurrentPasswordDoesNotMatchException.class,
                () -> userService.changePassword(1L, command));

        // then
        assertEquals("Aktualne hasło jest niepoprawne", exception.getMessage());
        assertEquals(oldHash, user.getPasswordHash());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_NewPasswordSameAsCurrent_ThrowsUserNewPasswordIsTheSameException() {
        // given
        User user = user(1L, "jan@example.com", passwordEncoder.encode("haslo123"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordUserCommand command = new ChangePasswordUserCommand("haslo123", "haslo123");

        // when
        UserNewPasswordIsTheSameException exception = assertThrows(UserNewPasswordIsTheSameException.class,
                () -> userService.changePassword(1L, command));

        // then
        assertEquals("Nowe hasło musi być inne niż aktualne", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_UserNotAssigned_DeletesUser() {
        // given
        User user = user(1L, "jan@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(patientRepository.existsByUserId(1L)).thenReturn(false);
        when(doctorRepository.existsByUserId(1L)).thenReturn(false);

        // when
        userService.delete(1L);

        // then
        verify(userRepository).delete(user);
    }

    @Test
    void delete_UserNotFound_ThrowsUserNotFoundException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.delete(1L));

        // then
        assertEquals("Nie znaleziono użytkownika: 1", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void delete_UserAssignedToPatient_ThrowsUserAlreadyAssignedException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "jan@example.com", "hash")));
        when(patientRepository.existsByUserId(1L)).thenReturn(true);

        // when
        UserAlreadyAssignedException exception = assertThrows(UserAlreadyAssignedException.class, () -> userService.delete(1L));

        // then
        assertEquals("Użytkownik 1 jest powiązany z pacjentem", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void delete_UserAssignedToDoctor_ThrowsUserAlreadyAssignedException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "ewa@example.com", "hash")));
        when(patientRepository.existsByUserId(1L)).thenReturn(false);
        when(doctorRepository.existsByUserId(1L)).thenReturn(true);

        // when
        UserAlreadyAssignedException exception = assertThrows(UserAlreadyAssignedException.class, () -> userService.delete(1L));

        // then
        assertEquals("Użytkownik 1 jest powiązany z lekarzem", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    // Encje nie mają setId(), a User.from() zostawia id = null.
    private static User user(Long id, String email, String passwordHash) {
        User user = User.from(new CreateUserCommand(email, "haslo123"), passwordHash);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
