package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.User
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime

@RestController
class SolverController {

    @PostMapping(value = ["/mailtime"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(@RequestBody input: SolverInput): ResponseEntity<Output> = Output(
        response = emptyMap()
    ).let { ResponseEntity.ok(it) }
}

data class SolverInput(val emails: List<SolverEmail>, val users: List<User>)

data class SolverEmail(val subject: String, val sender: String, val receiver: String, val timeSent: ZonedDateTime)
