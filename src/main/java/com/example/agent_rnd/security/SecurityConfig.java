package com.example.agent_rnd.security;

import com.example.agent_rnd.repository.UserRepository;
import com.example.agent_rnd.service.LogoutService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final LogoutService logoutService;

    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            LogoutService logoutService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.logoutService = logoutService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, userRepository, logoutService);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 누구나 접속 가능한 곳 (로그인, 회원가입 등)
                        .requestMatchers(
                                "/",
                                "/error",
                                "/favicon.ico",
                                "/api/auth/**",
                                "/api/login",
                                "/api/check-email",
                                "/api/send-code",
                                "/api/invites/validate",
                                "/api/notices/**",
                                "/api/payments/webhook"
                        ).permitAll()

                        // ★ [수정] "내 정보 조회"는 로그인한 누구나 가능하게 풀어줌! (관리자 설정보다 먼저 와야 함)
                        .requestMatchers("/api/users/me").authenticated()

                        // 2. 관리자만 접속 가능한 곳 (나머지 /api/users/** 포함)
                        .requestMatchers("/api/admin/**", "/api/users/**").hasRole("ADMIN")

                        // 3. 그 외 모든 요청은 로그인 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 팀장님이 확인하신 대로 프론트엔드 주소 지정
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}