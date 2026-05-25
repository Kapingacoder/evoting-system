package com.evoting.system;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import com.evoting.system.model.Role;
import com.evoting.system.model.User;
import com.evoting.system.repository.UserRepository;


@SpringBootApplication
@EnableAsync
public class EvotingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvotingSystemApplication.class, args);
	}
	@Bean
CommandLineRunner run(UserRepository userRepository) {
    return args -> {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User(
                    "admin",
                    "admin123",
                    Role.ADMIN
            );
            userRepository.save(admin);
            System.out.println("✅ Admin user created");
        }
    };
}


}
