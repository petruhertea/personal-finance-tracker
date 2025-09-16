package com.petruth.personal_finance_tracker.security;

import com.petruth.personal_finance_tracker.service.CustomUserDetailsService;
import com.petruth.personal_finance_tracker.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomUserDetailsService userService) throws Exception {
        http.authorizeHttpRequests(configurer ->
                configurer
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/transactions/**").authenticated()
                        .anyRequest().permitAll()

        );

        http.userDetailsService(userService);
        //Use default HTTP auth
        http.httpBasic(Customizer.withDefaults());

        //Disable CSRF
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}
