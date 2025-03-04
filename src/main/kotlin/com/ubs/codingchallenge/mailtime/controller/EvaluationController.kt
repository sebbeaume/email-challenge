package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.service.CoordinatorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ubs.codingchallenge.mailtime.model.EvaluationRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class EvaluationController(private val coordinatorService: CoordinatorService) {
    val coroutineScope = CoroutineScope(Dispatchers.Default)

    @PostMapping(value = ["/evaluate"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(@RequestBody evaluationRequest: EvaluationRequest): ResponseEntity<Void> =
        ResponseEntity<Void>(HttpStatus.ACCEPTED).also { coroutineScope.launch { coordinatorService(evaluationRequest) } }
}