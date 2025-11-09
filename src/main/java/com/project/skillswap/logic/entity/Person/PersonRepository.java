package com.project.skillswap.logic.entity.Person;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PersonRepository extends JpaRepository<Person, Long> {


    Optional<Person> findByEmail(String email);


    Optional<Person> findByGoogleOauthId(String googleOauthId);

    Optional<Person> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
