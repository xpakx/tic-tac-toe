package io.github.xpakx.tictactoe.security;

import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends GenericFilterBean {
    private final JwtUtils jwt;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if(request instanceof HttpServletRequest httpRequest && !shouldNotFilter(httpRequest)) {
            doFilterInternal(httpRequest);
        }
        filterChain.doFilter(request, response);
    }

    private void doFilterInternal(HttpServletRequest request) {
        try {
            authenticateUser(request);
            logger.debug("User authenticated");
        } catch(ExpiredJwtException ex) {
            logger.warn("JWT token is expired");
        } catch(UnsupportedJwtException ex) {
            logger.warn("JWT token is unsupported");
        } catch(MalformedJwtException ex) {
            logger.warn("JWT token is malformed");
        }
    }

    private void authenticateUser(HttpServletRequest request) {
        if(this.isAuthMissing(request)) {
            logger.warn("Authorization header is missing in request");
            return;
        }

        final String token = this.getAuthHeader(request).substring(7);
        logger.debug("Token is "+ token);

        if(jwt.isInvalid(token)) {
            logger.warn("Authorization token is invalid");
            return;
        }

        Claims claims = jwt.getAllClaimsFromToken(token);
        logger.debug("Claims received from token");

        if(claims != null && claims.getSubject() != null && !isUserAlreadyAuthenticated()) {
            UserDetails userDetails = createUserDetails(claims);
            logger.debug("UserDetails object for user " + userDetails.getUsername() + " created");
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.debug("UsernamePasswordAuthenticationToken loaded in Security Context");
        }
    }

    private boolean isUserAlreadyAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private UserDetails createUserDetails(Claims claims) {
        return new User(claims.getSubject(), "", getAuthoritiesFromClaims(claims));
    }

    private List<SimpleGrantedAuthority> getAuthoritiesFromClaims(Claims claims) {
        List<String> authoritiesString = (List<String>) claims.get("roles", List.class);
        return authoritiesString != null ? authoritiesString.stream().map(SimpleGrantedAuthority::new).toList() : new ArrayList<>();
    }

    private boolean isAuthMissing(HttpServletRequest request) {
        final String requestTokenHeader = this.getAuthHeader(request);
        return requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ");
    }

    private String getAuthHeader(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    private boolean shouldNotFilter(HttpServletRequest request) {
        return false;
    }
}
