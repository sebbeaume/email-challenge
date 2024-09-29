package com.ubs.codingchallenge.mailtime.controller

import com.ubs.codingchallenge.mailtime.model.Email
import com.ubs.codingchallenge.mailtime.model.Input
import com.ubs.codingchallenge.mailtime.model.MailtimeSolver
import com.ubs.codingchallenge.mailtime.model.Output
import com.ubs.codingchallenge.mailtime.model.User
import com.ubs.codingchallenge.mailtime.model.partTwo
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
class SolverController {

    @PostMapping(value = ["/mailtime"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluate(@RequestBody solverInput: SolverInput): ResponseEntity<Output> =
        solverInput.toInput.let(MailtimeSolver(partTwo)).let { ResponseEntity.ok(it) }
}

data class SolverInput(val emails: List<SolverEmail>, val users: List<User>) {
    val toInput: Input
        get() {
            return users.associateBy(User::name).let { userByName ->
                Input(
                    emails = emails.associateWith { userByName.getValue(it.sender) }.map { (email, sender) ->
                        Email(
                            subject = email.subject,
                            senderUser = sender,
                            receiverUser = userByName.getValue(email.receiver),
                            timeSent = email.timeSent.atZoneSameInstant(sender.officeHours.timeZone)
                        )
                    },
                    users = users
                )
            }
        }
}

data class SolverEmail(val subject: String, val sender: String, val receiver: String, val timeSent: OffsetDateTime)
