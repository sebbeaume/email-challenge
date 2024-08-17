package com.ubs.codingchallenge.mailtime.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@Profile("!production")
class NonProdWebSecurityConfig {

    @Bean
    fun configure(httpSecurity: HttpSecurity): SecurityFilterChain =
        httpSecurity.csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .httpBasic(Customizer.withDefaults())
            .build()
}
