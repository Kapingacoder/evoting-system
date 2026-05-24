package com.evoting.system.config;

import com.evoting.system.filter.JwtAuthFilter;
import com.evoting.system.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler; // Inject handler yetu mpya
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, CustomAuthenticationSuccessHandler successHandler, JwtAuthFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler; // Weka handler
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/admin/candidates/delete/**") // Allow delete without CSRF
                .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints (mobile app)
            )
            .authorizeHttpRequests(auth -> auth
                // Ruhusu kurasa za msingi, static resources, na makosa
                .requestMatchers("/", "/index", "/login", "/register", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // API endpoints for mobile app
                .requestMatchers("/api/auth/**").permitAll() // Allow login without authentication
                // New endpoints for password and username changes
                .requestMatchers("/api/voter/change-password").authenticated()
                .requestMatchers("/api/admin/change-password").authenticated()
                .requestMatchers("/api/admin/change-username").authenticated()
                .requestMatchers("/api/admin/profile").authenticated()
                // Existing wildcard patterns
                .requestMatchers("/api/voter/**").authenticated() // Require authentication for voter API
                .requestMatchers("/api/admin/**").authenticated() // Require authentication for admin API
                // Web endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/voter/**").hasRole("VOTER")
                .anyRequest().authenticated()
            )
            // Muhimu — API endpoints zitumie STATELESS (JWT, sio session)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            // Ongeza JWT filter kabla ya Spring Security filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler) // Tumia handler yetu maalum
                .permitAll()
            )
            .logout(logout -> {
                logout.logoutUrl("/logout")
                      .logoutSuccessUrl("/login?logout")
                      .invalidateHttpSession(true)
                      .deleteCookies("JSESSIONID")
                      .permitAll();
                // Allow both GET and POST for logout
                logout.logoutRequestMatcher(request -> {
                    String method = request.getMethod();
                    return ("GET".equals(method) || "POST".equals(method)) && "/logout".equals(request.getRequestURI());
                });
            })
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}