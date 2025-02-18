package com.valensas.common.kafka.webflux.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import reactor.util.retry.Retry
import java.time.Duration

@ConfigurationProperties(prefix = "spring.kafka.retry")
data class RetryConfigurationProperties(
    val type: RetryType = RetryType.Backoff,
    val fixedDelay: FixedDelayProperties,
    val backoff: BackoffProperties
) {
    enum class RetryType {
        FixedDelay,
        Backoff,
        Indefinitely
    }

    data class FixedDelayProperties(
        val maxAttempts: Long = Long.MAX_VALUE,
        val delay: Duration = Duration.ofSeconds(1)
    )

    data class BackoffProperties(
        val maxAttempts: Long = Long.MAX_VALUE,
        val minBackoff: Duration = Duration.ofMillis(100),
        val maxBackoff: Duration = Duration.ofMinutes(1)
    )

    fun build(): Retry =
        when (type) {
            RetryType.FixedDelay -> Retry.fixedDelay(fixedDelay.maxAttempts, fixedDelay.delay)
            RetryType.Backoff -> Retry.backoff(backoff.maxAttempts, backoff.minBackoff).maxBackoff(backoff.maxBackoff)
            RetryType.Indefinitely -> Retry.indefinitely()
        }
}
