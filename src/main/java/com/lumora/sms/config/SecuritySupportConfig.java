package com.lumora.sms.config;
import org.springframework.context.annotation.*; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder;
/** Provides password hashing for account services; route security is added in a later stage. */
@Configuration public class SecuritySupportConfig { @Bean public PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); } }
