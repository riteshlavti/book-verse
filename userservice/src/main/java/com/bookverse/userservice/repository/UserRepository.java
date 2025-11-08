package com.bookverse.userservice.repository;

import com.bookverse.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByEmailId(String emailId);
    User findByUsername(String username);
}
