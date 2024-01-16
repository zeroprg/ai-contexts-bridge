package com.bloberryconsulting.aicontextsbridge.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Value("${ui.uri}")
    private String uiUri;

    public final static String ROLE_CUSTOMER_DESCR = " This operation is only available to users with the 'ROLE_CUSTOMER' role, which typically includes only a new API creation/selection/query";
    public final static String ROLE_SITE_ADMINISTRATOR_DESCR = " This operation is only available to users with the 'ROLE_SITE_ADMINISTRATOR' role, which typically includes creation/update/query all site profiles except not owned APIs ";
    public final static String ROLE_CLIENT_ADMINISTRATOR_DESC = " This operation is only available to users with the 'ROLE_CLIENT_ADMINISTRATOR' role, which typically includes creation/update/query own client's (company's)) users and client's (company's) own APIs ";
    public final static String ROLE_APIKEY_MANAGER_DESC = "This operation is only available to users with the 'ROLE_APIKEY_MANAGER' role, which typically includes view only of API keys of his own and asigning a new role to a new users in system";

    private final CustomOidcUserService customOidcUserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    public SecurityConfiguration(CustomOidcUserService customOidcUserService, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandle) {
        this.customOidcUserService = customOidcUserService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandle;
    }

    @Bean
    @Profile("!no-security") // Apply this configuration when 'no-security' profile is not active
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()               
              
                .authorizeRequests(requests -> requests
                        .antMatchers("/","/index.html","/actuator/**", "/loginerror.html", "/success.html").permitAll()
                        //.antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html","/api-docs/**", "/webjars/**").permitAll()
                        .antMatchers( "/public/**").permitAll()
                        .antMatchers("/stripepayment/webhook").permitAll() // Allow webhook endpoint
                        .antMatchers("/stripepayment/create-checkout-session").permitAll()
                        .antMatchers("/ws/**").permitAll()
                        .antMatchers("/topic/transription").permitAll()
                        .antMatchers("/audio").permitAll()


                        .antMatchers("/api/keys/manage/**").hasRole("APIKEY_MANAGER")                        
                        .antMatchers("/api/admin/profiles/**").hasRole("SITE_ADMINISTRATOR")
                        .antMatchers("/api/client/**").hasRole("CLIENT_ADMINISTRATOR")
                        .antMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .userInfoEndpoint() 
                        .oidcUserService(customOidcUserService)
                        .and()
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customFailureHandler()))
                        
                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession());
               

        return http.build();
    }

    @Bean
    @Profile("no-security") // Apply this configuration only when 'no-security' profile is active
    public SecurityFilterChain noSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests(requests -> requests
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable()); // Disable CSRF for no-security profile
        return http.build();
    }

    private AuthenticationFailureHandler customFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("/loginerror.html");
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(uiUri)); // Specify your frontend origin
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true); // Important for cookies
    
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all endpoints

        return source;
    }
}
