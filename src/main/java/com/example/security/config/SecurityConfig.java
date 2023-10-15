package com.example.security.config;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true) // secured 어노테이션 활성화, preAuthorize 어노테이션 활성화
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder encodePwd() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/user/**")).authenticated() // 인증만 되면 접근 가능
                .requestMatchers(new AntPathRequestMatcher("/manager/**")).hasAnyRole("ADMIN","MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole("ADMIN")
                .anyRequest().permitAll());

        // 로그인 관련 설정
        http.formLogin(form -> form
                .loginPage("/loginForm").permitAll()
                .loginProcessingUrl("/login") // security가 대신 로그인 진행
                .defaultSuccessUrl("/"));

        // 세션 관련 설정
        http.sessionManagement(session -> session
                .sessionFixation().changeSessionId() // 세션 고정 공격 방어
                .maximumSessions(1) // 동시 허용 가능 세션 수
//                .expiredSessionStrategy()
                .maxSessionsPreventsLogin(true) // 두번째 로그인 거부됨, 만약 false면 두번째 로그인 시 첫 번째 로그인을 무효화 시킴
                .sessionRegistry(sessionRegistry()) // 동시에 로그인한 세션들을 추적하고 관리
                .expiredUrl("/loginForm") // 세션이 만료된 후 이동 할 페이지
        );

        return http.build();
    }

    // SessionRegistry 컴포넌트 추가, 중복 로그인 방지를 위한 여러 세션이 열리지 않도록 하기
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // HttpSessionEventPublisher 컴포넌트 추가, HttpSession 이벤트를 Spring 이벤트로 변환
    // 세션 생성 및 소멸 이벤트 처리 용도
    @Bean
    public static ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }
}
