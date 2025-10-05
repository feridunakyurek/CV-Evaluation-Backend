package com.cvanalyzer.services;

import com.cvanalyzer.entities.Role;
import com.cvanalyzer.entities.User;
import com.cvanalyzer.repos.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Döngü hatası yaşamamak için @Lazy eklendi
    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Spring Security’nin kimlik doğrulama için çağırdığı method
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities((GrantedAuthority) Collections.singleton(user.getRole().name()))
                .build();
    }

    // Yeni kullanıcı kaydı
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername()))
            throw new RuntimeException("Bu kullanıcı adı zaten alınmış");
        if (userRepository.existsByEmail(user.getEmail()))
            throw new RuntimeException("Bu e-posta zaten kayıtlı");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

    // Admin eklemek istersen (opsiyonel)
    public User registerAdmin(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ADMIN);
        return userRepository.save(user);
    }
}
