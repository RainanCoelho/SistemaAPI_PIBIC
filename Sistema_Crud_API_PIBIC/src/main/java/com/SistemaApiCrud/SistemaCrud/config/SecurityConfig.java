package com.SistemaApiCrud.SistemaCrud.config;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource,
            @Value("${app.docs.public:false}") boolean publicDocs,
            @Value("${app.security.require-https:false}") boolean requireHttps) throws Exception {
        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll();

                    if (publicDocs) {
                        auth.requestMatchers(HttpMethod.GET, "/openapi.yaml").permitAll();
                    } else {
                        auth.requestMatchers(HttpMethod.GET, "/openapi.yaml").hasRole("ADMIN");
                    }

                    auth.requestMatchers("/usuarios/**").hasRole("ADMIN")
                            .requestMatchers("/casos/**").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers("/perguntas/**").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers("/pacientes/**").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers("/conteudos/**").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers(HttpMethod.POST, "/alunos/*/casos/*/responder").hasRole("ALUNO")
                            .requestMatchers(HttpMethod.GET, "/alunos/*/casos/*/completo").hasRole("ALUNO")
                            .requestMatchers(HttpMethod.GET, "/alunos/*/casos-disponiveis").hasAnyRole("ADMIN", "ALUNO")
                            .requestMatchers(HttpMethod.GET, "/alunos/*/historico").hasAnyRole("ADMIN", "ALUNO")
                            .requestMatchers(HttpMethod.GET, "/alunos/*/desempenho").hasAnyRole("ADMIN", "ALUNO")
                            .requestMatchers(HttpMethod.GET, "/alunos/*").hasAnyRole("ADMIN", "ALUNO")
                            .requestMatchers(HttpMethod.PUT, "/alunos/*").hasAnyRole("ADMIN", "ALUNO")
                            .requestMatchers("/alunos/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.GET, "/professores/*/casos").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers(HttpMethod.GET, "/professores/*/relatorio-desempenho").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers(HttpMethod.GET, "/professores/*").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers(HttpMethod.PUT, "/professores/*").hasAnyRole("ADMIN", "PROFESSOR")
                            .requestMatchers("/professores/**").hasRole("ADMIN")
                            .anyRequest().denyAll();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(splitProperty(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> splitProperty(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
