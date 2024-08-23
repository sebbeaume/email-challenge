package com.ubs.codingchallenge.mailtime.service

import com.ubs.codingchallenge.mailtime.config.AppConfig
import com.ubs.codingchallenge.mailtime.config.objectMapper
import com.ubs.codingchallenge.mailtime.model.ChallengeRequest
import com.ubs.codingchallenge.mailtime.model.ChallengeResponse
import com.ubs.codingchallenge.mailtime.model.ChallengeRun
import com.ubs.codingchallenge.mailtime.model.EvaluationRequest
import com.ubs.codingchallenge.mailtime.model.EvaluationResultRequest
import com.ubs.codingchallenge.mailtime.model.EvaluatorService
import com.ubs.codingchallenge.mailtime.model.RequestPayload
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CoordinatorService(
    val appConfig: AppConfig,
    val httpClient: OkHttpClient,
    val evaluatorService: EvaluatorService
) {
    private val logger: Logger = LoggerFactory.getLogger(CoordinatorService::class.java)

    operator fun invoke(evaluationRequest: EvaluationRequest) {
        val result = evaluatorService.evaluateTeam(evaluationRequest.asRun)
            .let { EvaluationResultRequest(evaluationRequest.runId, it.score, it.message) }
        try {
            result.post(evaluationRequest.callbackUrl.toHttpUrl()) { client, response ->
                client to response.addHeader("Authorization", appConfig.bearerToken)
            }?.also { logger.info("Notified coordinator with: {}", result) }
                ?: logger.warn("Error notifying coordinator with: {}", result)
        } catch (e: Exception) {
            logger.error("Error notifying coordinator with: $result\nException message: ${e.message}")
        }
    }

    private val EvaluationRequest.asRun: ChallengeRun
        get() = object : ChallengeRun {
            override val teamUrl: String = this@asRun.teamUrl

            override fun invoke(challengeRequest: ChallengeRequest): ChallengeResponse? =
                challengeRequest.also { logger.debug("Evaluating [$runId] $teamUrl with: ${it.toJson}") }
                    .post(teamUrl.toHttpUrl() + appConfig.endpointSuffix) { client, response ->
                        client.readTimeout(Duration.ofSeconds(1)) to response
                    }?.use { it.body?.string()?.let(evaluatorService::convert) }
                    ?.also { logger.debug("Got response for [{}] {}: {}", runId, teamUrl, it) }
        }

    private val AppConfig.bearerToken: String get() = "Bearer $coordinatorAuthToken"

    private val RequestPayload.toJson: String get() = objectMapper.writeValueAsString(this)

    private fun RequestPayload.post(
        url: HttpUrl,
        modifier: (OkHttpClient.Builder, Request.Builder) -> Pair<OkHttpClient.Builder, Request.Builder>
    ): Response? =
        modifier.invoke(
            httpClient.newBuilder(),
            Request.Builder().post(this.toJson.toRequestBody(MEDIA_TYPE_JSON)).url(url)
        ).let { (clientBuilder, requestBuilder) -> clientBuilder.build().newCall(requestBuilder.build()).execute() }
            .let { if (it.isSuccessful) it else it.close().run { null } }

    private operator fun HttpUrl.plus(suffix: String) = newBuilder().addPathSegment(suffix).build()

    companion object {
        private val MEDIA_TYPE_JSON = "application/json".toMediaType()
    }
}
