package com.khushi.stemquest.config;

import com.khushi.stemquest.model.User;
import com.khushi.stemquest.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String code) throws UsernameNotFoundException {
        User u = repo.findByStudentCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new UsernameNotFoundException(code));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.studentCode)
                .password(u.password)
                .roles(u.role)
                .build();
    }
}
