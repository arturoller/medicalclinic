package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.UpdateUserCommand;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Setter(AccessLevel.NONE)
    private String passwordHash;

    public void update(UpdateUserCommand command) {
        setEmail(command.email());
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }

    public static User from(CreateUserCommand command, String passwordHash) {
        return new User(null, command.email(), passwordHash);
    }
}
