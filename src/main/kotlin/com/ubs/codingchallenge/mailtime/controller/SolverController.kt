package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.model.Input
import com.ubs.codingchallenge.mailtime.model.Output
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SolverController {

    @PostMapping(value = ["/mailtime"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(@RequestBody input: Input): ResponseEntity<Output> = Output(
        response = emptyMap()
    ).let { ResponseEntity.ok(it) }
}
