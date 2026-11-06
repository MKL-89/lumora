package com.lumora.sms.security;
import com.lumora.sms.repository.UserRepository; import lombok.RequiredArgsConstructor; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
/** Resolves sign-in users and their role authorities from the Lumora database. */
@Service @RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService { private final UserRepository users;
 @Override @Transactional(readOnly=true) public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { return LumoraUserPrincipal.from(users.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("Invalid username or password."))); }
}
