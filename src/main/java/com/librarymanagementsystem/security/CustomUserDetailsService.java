package com.librarymanagementsystem.security;

import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //Tìm user trong DB
        User user = userRepository.findByUserName(username).orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng!"));

        // Trả về đối tượng User của Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password(user.getPassword())
                .authorities(user.getRole().getRoleName().name())
                .build();
    }
}