package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.model.DifficultyLevel
import com.ubs.codingchallenge.mailtime.model.Input
import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.generateInput
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ExampleController {

    @GetMapping(value = ["/example"])
    fun example(): ResponseEntity<ExampleResponse> =
        generateInput(DifficultyLevel.EXAMPLE)
            .let { input ->
                ExampleResponse(
                    input = input,
                    output = input.users.associate { it.name to it.responseTimes.sum() }.let(::Output)
                )
            }.let { ResponseEntity.ok(it) }
}

data class ExampleResponse(val input: Input, val output: Output)
