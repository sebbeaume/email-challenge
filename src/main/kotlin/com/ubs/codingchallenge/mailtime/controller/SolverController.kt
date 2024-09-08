package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.model.OfficeHours
import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.User
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters.next
import kotlin.math.roundToLong

@RestController
class SolverController {

    @PostMapping(value = ["/mailtime"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(@RequestBody input: SolverInput): ResponseEntity<Output> =
        input.let(MailtimeSolver(partTwo)).let(::Output).let { ResponseEntity.ok(it) }
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
                println("DEBUG: $current")
                results.merge(
                    current.sender,
                    SubPeriod(
                        duration = calculator(userByName.getValue(current.sender), previous.timeSent, current.timeSent)
                            .also { println("\t${current.sender} + ${it.seconds}") },
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

private val partTwo: (User, OffsetDateTime, OffsetDateTime) -> Duration =
    { user, previous, current ->
        println("\tDEBUG: CALCULATING FOR [${user.name}, ${user.officeHours}]: $previous .. $current")
        val (zonedPrevious, zonedCurrent) = user.officeHours(previous) to user.officeHours(current)
        generateSequence(DateTimeFromTo(officeHours = user.officeHours, from = null, to = zonedPrevious)) {
            it.until(cutOff = zonedCurrent)
        }.takeWhile { it.to <= zonedCurrent }
            .mapNotNull { it.toDuration }
            .reduce { a, b -> a + b }
    }

private data class DateTimeFromTo(val officeHours: OfficeHours, val from: ZonedDateTime?, val to: ZonedDateTime) {
    val toDuration: Duration? = from?.let { Duration.between(it, to) }

    fun until(cutOff: ZonedDateTime): DateTimeFromTo? =
        this.copy(
            from = to.takeIf { it.hour in officeHours.start until officeHours.end },
            to = to.with(officeHours.asTemporalAdjuster).coerceAtMost(cutOff)
        ).takeIf { it.toDuration == null || it.toDuration > Duration.ZERO }
            .also { println("\t\t$this") }

    override fun toString(): String = "${from?.let(::format) ?: "".padEnd(24, ' ')} .. ${format(to)}"

    private fun format(zonedDateTime: ZonedDateTime) = zonedDateTime.toOffsetDateTime().format(formatter)

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ")
    }
}

private operator fun OfficeHours.invoke(offsetDateTime: OffsetDateTime): ZonedDateTime =
    offsetDateTime.toInstant().atZone(timeZone)

private val OfficeHours.asTemporalAdjuster: TemporalAdjuster
    get() = TemporalAdjuster { temporal ->
        ZonedDateTime.from(temporal).let {
            when (it.dayOfWeek) {
                SATURDAY, SUNDAY -> it.with(next(MONDAY)).withHour(start)
                else -> {
                    when {
                        it.hour < start ->
                            it.withHour(start)

                        it.hour >= end ->
                            it.with(next(it.dayOfWeek.nextWeekday)).withHour(start)

                        else -> it.withHour(end)
                    }
                }
            }
        }.withMinute(0).withSecond(0).withNano(0)
    }

private val DayOfWeek.nextWeekday: DayOfWeek get() = if (this >= FRIDAY) MONDAY else this.plus(1)
