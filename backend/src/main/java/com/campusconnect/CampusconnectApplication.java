package com.campusconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * CampusConnect - College Symposium and Workshop Management System.
 *
 * <p>{@code UserDetailsServiceAutoConfiguration} is excluded on purpose. Boot would
 * otherwise create an in-memory user with a randomly generated password on every
 * start, which is both unused (authentication is handled by {@code AuthService} and
 * the JWT filter) and misleading in the logs of a security-sensitive application.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CampusconnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusconnectApplication.class, args);
    }
}
