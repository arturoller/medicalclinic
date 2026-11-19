package com.arturoller.medicalclinic.repository;

import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    boolean existsByUserId(Integer userId);

    Optional<User> findByIdCardNo(String idCardNo);
}
