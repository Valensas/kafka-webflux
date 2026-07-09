package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.tracing.KafkaTracing
import io.opentelemetry.api.OpenTelemetry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnClass(OpenTelemetry::class)
class KafkaTracingAutoConfiguration {
    @Bean
    fun kafkaTracing(openTelemetry: ObjectProvider<OpenTelemetry>): KafkaTracing? = openTelemetry.ifAvailable?.let { KafkaTracing(it) }
}
