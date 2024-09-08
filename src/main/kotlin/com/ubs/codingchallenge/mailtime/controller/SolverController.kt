package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.User
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.math.roundToLong

@RestController
class SolverController {

    @PostMapping(value = ["/mailtime"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(@RequestBody input: SolverInput): ResponseEntity<Output> =
        input.let(MailtimeSolver(partOne)).let(::Output).let { ResponseEntity.ok(it) }
}

data class SolverInput(val emails: List<SolverEmail>, val users: List<User>)

data class SolverEmail(val subject: String, val sender: String, val receiver: String, val timeSent: OffsetDateTime)

private class MailtimeSolver(private val calculator: (User, OffsetDateTime, OffsetDateTime) -> Duration) :
        (SolverInput) -> Map<String, Long> {
    override fun invoke(input: SolverInput): Map<String, Long> = input.run {
        users.associate { it.name to SubPeriod(duration = Duration.ZERO, count = 0) }.toMutableMap() to
                users.associateBy { it.name }
    }.let { (results, userByName) ->
        input.emails.groupBy { email -> email.subject.replace("RE: ", "") }.values.forEach { emails ->
            emails.sortedBy { it.timeSent }.reduce { previous, current ->
                results.merge(
                    current.sender,
                    SubPeriod(
                        duration = calculator(userByName.getValue(current.sender), previous.timeSent, current.timeSent),
                        count = 1
                    ),
                    SubPeriod::plus
                )
                current
            }
        }
        results.mapValues { (_, value) -> value() }
    }

    data class SubPeriod(val duration: Duration, val count: Int) : () -> Long {
        operator fun plus(other: SubPeriod) =
            SubPeriod(duration = this.duration + other.duration, count = this.count + other.count)

        override fun invoke(): Long = duration.seconds.toDouble().div(count).roundToLong()
    }
}

private val partOne: (User, OffsetDateTime, OffsetDateTime) -> Duration =
    { _, previous, current -> Duration.between(previous, current) }
