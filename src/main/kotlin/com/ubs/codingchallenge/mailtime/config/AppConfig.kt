package com.ubs.codingchallenge.mailtime.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.ubs.codingchallenge.mailtime.model.ChallengeLevel
import com.ubs.codingchallenge.mailtime.model.DifficultyLevel
import com.ubs.codingchallenge.mailtime.model.MailtimeChallenge
import com.ubs.codingchallenge.mailtime.model.MailtimeChecker
import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import java.time.Duration.ofSeconds
import java.util.EnumSet

@ConfigurationProperties("app")
data class AppConfig(val coordinatorAuthToken: String, val endpointSuffix: String) {
    @Bean
    fun httpClient() = OkHttpClient.Builder().callTimeout(ofSeconds(5)).build()

    @Bean
    fun challenge() = MailtimeChallenge

    @Bean
    fun checker() = MailtimeChecker

    @Bean
    fun levels(): () -> Iterable<ChallengeLevel> =
        { EnumSet.complementOf(EnumSet.of(DifficultyLevel.EXAMPLE)).shuffled() }
}

val objectMapper: ObjectMapper = ObjectMapper().registerModule(
    KotlinModule.Builder()
        .withReflectionCacheSize(512)
        .configure(KotlinFeature.NullToEmptyCollection, false)
        .configure(KotlinFeature.NullToEmptyMap, false)
        .configure(KotlinFeature.NullIsSameAsDefault, false)
        .configure(KotlinFeature.SingletonSupport, false)
        .configure(KotlinFeature.StrictNullChecks, false)
        .build()
)
