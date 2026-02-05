package com.example.agent_rnd.security;

import com.example.agent_rnd.repository.UserRepository;
import com.example.agent_rnd.service.LogoutService;
import com.example.agent_rnd.security.JwtAuthenticationFilter;
import com.example.agent_rnd.security.JwtTokenProvider;
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
                .cors(cors -> cors.configure(http)) // CORS 설정 활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 공용 API (로그인, 회원가입, 이메일 등) - 403 에러 방지
                        .requestMatchers(
                                "/",
                                "/error",
                                "/favicon.ico",
                                "/api/auth/**",       // ★ email, invite, company-signup 모두 포함
                                "/api/login",
                                "/api/check-email",   // (혹시 모를 레거시 경로)
                                "/api/send-code",
                                "/api/invites/validate",
                                "/api/notices/**",    // 공고 조회 허용
                                "/api/payments/webhook"
                        ).permitAll()

                        // 2. ★ 관리자 전용 API (일반 MEMBER 접근 불가)
                        .requestMatchers("/api/admin/**", "/api/users/**").hasRole("ADMIN")

                        // 3. 나머지는 인증 필요 (MEMBER, ADMIN 모두 접근 가능)
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