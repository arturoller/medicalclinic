package com.arturoller.medicalclinic.mapper;

import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.UserDto;
import com.arturoller.medicalclinic.model.User;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
    @Autowired
    private PasswordEncoder passwordEncoder;

    public abstract UserDto toDto(User user);

    public User toModel(CreateUserCommand command) {
        if (command == null) {
            return null;
        }

        return User.from(command, passwordEncoder.encode(command.password()));
    }
}
