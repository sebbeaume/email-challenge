package com.ubs.codingchallenge.mailtime

import com.ubs.codingchallenge.mailtime.config.AppConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppConfig::class)
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}
