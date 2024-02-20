package io.github.xpakx.tictactoe.user;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var userAccount = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username " + username));
        return new User(userAccount.getUsername(), userAccount.getPassword(), userAccount.getRoles());
    }

    public UserDetails userAccountToUserDetails(io.github.xpakx.tictactoe.user.User userAccount) {
        return new User(userAccount.getUsername(), userAccount.getPassword(), userAccount.getRoles());
    }
}
