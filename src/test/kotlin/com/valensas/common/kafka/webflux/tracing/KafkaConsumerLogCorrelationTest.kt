package com.valensas.common.kafka.webflux.tracing

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.valensas.common.kafka.webflux.consumer.PayloadKafkaConsumerDescriptor
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.kotlin.core.publisher.toFlux

class KafkaConsumerLogCorrelationTest {
    private lateinit var exporter: InMemorySpanExporter
    private lateinit var tracing: KafkaTracing
    private lateinit var logger: Logger
    private lateinit var capturedLines: MutableList<String>

    @BeforeEach
    fun setup() {
        exporter = InMemorySpanExporter.create()
        tracing =
            KafkaTracing(
                OpenTelemetrySdk
                    .builder()
                    .setTracerProvider(
                        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()
                    ).setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                    .build()
            )

        // Stands in for the agent's logback instrumentation: reads Span.current() on the
        // logging thread at append time and renders the same correlation prefix.
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        capturedLines = mutableListOf()
        val lines = capturedLines
        val capturing =
            object : AppenderBase<ILoggingEvent>() {
                override fun append(event: ILoggingEvent) {
                    val spanContext = Span.current().spanContext
                    val traceId = if (spanContext.isValid) spanContext.traceId else ""
                    lines += "[traceId=$traceId] ${event.formattedMessage}"
                }
            }.also {
                it.context = loggerContext
                it.start()
            }
        logger =
            loggerContext.getLogger("test.consumer").also {
                it.isAdditive = false
                it.addAppender(capturing)
            }
    }

    @AfterEach
    fun tearDown() {
        logger.detachAndStopAllAppenders()
    }

    // Mirrors what KafkaConsumerRegisterer does per record.
    private fun consume(handler: (String) -> Publisher<Unit>): String {
        val descriptor =
            PayloadKafkaConsumerDescriptor(
                topic = TOPIC,
                modelType = String::class,
                wildcard = false,
                consumer = handler
            )
        val record = ConsumerRecord<String, Any>(TOPIC, 0, 0, "key", PAYLOAD)

        tracing.traceConsume(record.topic(), record.headers(), descriptor.invoke(record).toFlux()).blockLast()

        // span.end() runs in doFinally on the completing thread, after blockLast unblocks
        val deadline = System.currentTimeMillis() + 5_000
        while (exporter.finishedSpanItems.none { it.kind == SpanKind.CONSUMER } && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        return exporter.finishedSpanItems.first { it.kind == SpanKind.CONSUMER }.traceId
    }

    @Test
    fun `kafka consumer log contains the trace id`() {
        val traceId =
            consume(
                suspendConsumer { payload ->
                    withContext(Dispatchers.Default) {
                        logger.info("handling {}", payload)
                    }
                }
            )

        assertEquals("[traceId=$traceId] handling $PAYLOAD", capturedLines.single())
    }

    // Documents why suspendConsumer exists: a handler bridged with a plain mono loses the
    // span on dispatcher hops, so the correlation pattern renders an empty trace id.
    @Test
    fun `plain mono handler logs without a trace id`() {
        consume { payload ->
            mono {
                withContext(Dispatchers.Default) {
                    logger.info("handling {}", payload)
                }
            }
        }

        assertEquals("[traceId=] handling $PAYLOAD", capturedLines.single())
    }

    companion object {
        private const val TOPIC = "test-topic"
        private const val PAYLOAD = "payload-1"
    }
}
