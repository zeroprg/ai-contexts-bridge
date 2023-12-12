package com.bloberryconsulting.aicontextsbridge.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityUtils {

    /**
     * Adds a role to the current user if it does not already exist.
     * 
     * @param newRole The role to add.
     */
    public static void addRoleToCurrentUserIfNotExists(String newRole) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            Set<SimpleGrantedAuthority> currentAuthorities = currentAuth.getAuthorities().stream()
                .map(grantedAuthority -> new SimpleGrantedAuthority(grantedAuthority.getAuthority()))
                .collect(Collectors.toSet());

            if (!currentAuthorities.contains(new SimpleGrantedAuthority(newRole))) {
                currentAuthorities.add(new SimpleGrantedAuthority(newRole));

                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        currentAuth.getPrincipal(),
                        currentAuth.getCredentials(),
                        currentAuthorities);

                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }
        }
    }
}
