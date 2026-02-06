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
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 🔓 공용 접근 허용
                        .requestMatchers(
                                "/",
                                "/error",
                                "/favicon.ico",
                                "/api/login",
                                "/api/auth/**",
                                "/api/check-email",        // ✅ 이메일 중복 확인 추가
                                "/api/verify-email-code",  // ✅ 이메일 인증 코드 확인 추가
                                "/api/notices/**",         // 공고 목록/상세/다운로드 허용
                                "/api/payments/**",        // 결제 관련 API
                                "/api/invites/validate",   // 초대 토큰 확인
                                "/collect/**",             // 공고 수집
                                "/parse/**"                // 파일 파싱
                        ).permitAll()

                        // 🔒 그 외는 JWT 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}