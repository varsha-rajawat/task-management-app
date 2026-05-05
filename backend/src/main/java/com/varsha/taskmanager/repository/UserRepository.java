package com.varsha.taskmanager.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.varsha.taskmanager.model.User;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    //SELECT COUNT(*) > 0 FROM users WHERE email = ?
    boolean existsByEmail(String email);
}
