package com.khushi.stemquest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())   // HTML/JS/CSS pages are public so they can work offline
            .formLogin(f -> f
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((req, res, auth) -> {
                    boolean teacher = auth.getAuthorities().stream()
                            .anyMatch(x -> x.getAuthority().equals("ROLE_TEACHER"));
                    res.sendRedirect(teacher ? "/teacher.html" : "/index.html");
                })
                .failureUrl("/login.html?error=1")
                .permitAll())
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login.html"))
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401)));
        return http.build();
    }
}
