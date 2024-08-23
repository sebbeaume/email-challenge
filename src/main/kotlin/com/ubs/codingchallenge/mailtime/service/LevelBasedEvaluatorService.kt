package com.ubs.codingchallenge.mailtime.service

import com.ubs.codingchallenge.mailtime.model.ChallengeLevel
import com.ubs.codingchallenge.mailtime.model.ChallengeResult
import com.ubs.codingchallenge.mailtime.model.ChallengeRun
import com.ubs.codingchallenge.mailtime.model.Checker
import com.ubs.codingchallenge.mailtime.model.EvaluatorService
import com.ubs.codingchallenge.mailtime.model.LevelBasedChallenge
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LevelBasedEvaluatorService(
    override val challenge: LevelBasedChallenge,
    override val checker: Checker,
    val levels: () -> Iterable<ChallengeLevel>
) : EvaluatorService {
    private val logger: Logger = LoggerFactory.getLogger(LevelBasedEvaluatorService::class.java)

    override fun evaluateTeam(challengeRun: ChallengeRun): ChallengeResult = levels().mapNotNull { level ->
        try {
            with(challenge.createFor(challengeRun.teamUrl, level)) {
                challengeRun(this)?.let { checker.check(this, it) }
            }
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }.fold(ChallengeResult(), ChallengeResult::plus).let { it.copy(score = it.score) }
}
