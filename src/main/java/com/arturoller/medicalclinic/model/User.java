package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.UpdateUserCommand;
import com.arturoller.medicalclinic.util.EmailConverter;
import jakarta.persistence.*;
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
    private Integer id;

    @Convert(converter = EmailConverter.class)
    private Email email;

    @Setter(AccessLevel.NONE)
    private String passwordHash;

    private String idCardNo;

    public void update(UpdateUserCommand command) {
        setEmail(command.email());
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }

    public static User from(CreateUserCommand command, String passwordHash) {
        return new User(null, command.email(), passwordHash, command.idCardNo());
    }
}
