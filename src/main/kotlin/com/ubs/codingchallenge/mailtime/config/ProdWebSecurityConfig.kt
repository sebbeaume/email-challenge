package com.ubs.codingchallenge.mailtime.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import kotlin.random.Random
import kotlin.random.nextInt

@Configuration
@EnableWebSecurity
@Profile("production")
class ProdWebSecurityConfig {

    @Bean
    fun auth(auth: AuthenticationManagerBuilder) =
        List(20) { Random.nextInt(33..126) }.joinToString("")
            .also(::println)
            .let(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
            .let { User.withUsername("notAdmin").password(it).roles("ADMIN").build() }
            .let { InMemoryUserDetailsManager(it) }

    @Bean
    fun configure(httpSecurity: HttpSecurity): SecurityFilterChain =
        httpSecurity.csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/", "/README.md", "/favicon.ico", "/evaluate", "/example").permitAll()
                    .anyRequest().permitAll()
            }.httpBasic(Customizer.withDefaults())
            .build()
}
