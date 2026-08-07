package com.valensas.common.kafka.webflux.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context as ReactorContext

class KafkaTracingTest {
    private lateinit var exporter: InMemorySpanExporter
    private lateinit var openTelemetry: OpenTelemetry
    private lateinit var tracing: KafkaTracing

    @BeforeEach
    fun setup() {
        exporter = InMemorySpanExporter.create()
        openTelemetry =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(
                    SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()
                ).setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build()
        tracing = KafkaTracing(openTelemetry)
    }

    @Test
    fun `produce records a producer span and injects context into headers`() {
        val headers = RecordHeaders()

        tracing.produce("orders", ReactorContext.empty(), headers)

        assertNotNull(headers.lastHeader("traceparent"))
        val spans = exporter.finishedSpanItems
        assertEquals(1, spans.size)
        assertEquals(SpanKind.PRODUCER, spans[0].kind)
        assertEquals("orders publish", spans[0].name)
    }

    @Test
    fun `produce continues the parent span current at call time`() {
        val parent = openTelemetry.getTracer("test").spanBuilder("request").startSpan()

        val headers = RecordHeaders()
        parent.makeCurrent().use { tracing.produce("orders", ReactorContext.empty(), headers) }
        parent.end()

        val producer = exporter.finishedSpanItems.first { it.kind == SpanKind.PRODUCER }
        assertEquals(parent.spanContext.traceId, producer.traceId)
        assertEquals(parent.spanContext.spanId, producer.parentSpanId)
    }

    @Test
    fun `suspendConsumer keeps the consumer span current across dispatcher hops`() {
        val headers = RecordHeaders()
        tracing.produce("orders", ReactorContext.empty(), headers)

        var traceIdInCoroutine: String? = null
        val consumer =
            suspendConsumer<String> {
                withContext(Dispatchers.Default) {
                    traceIdInCoroutine = Span.current().spanContext.traceId
                }
            }

        tracing.traceConsume("orders", headers, Flux.from(consumer("payload"))).blockLast()

        val consumerSpan = exporter.finishedSpanItems.first { it.kind == SpanKind.CONSUMER }
        assertNotNull(traceIdInCoroutine)
        assertEquals(consumerSpan.traceId, traceIdInCoroutine)
    }

    @Test
    fun `suspendConsumer runs without a tracing context`() {
        var invoked = false
        val consumer = suspendConsumer<String> { invoked = true }

        Mono.from(consumer("payload")).block()

        assertEquals(true, invoked)
    }

    @Test
    fun `traceConsume records a consumer span linked to the producer`() {
        val producerHeaders = RecordHeaders()
        tracing.produce("orders", ReactorContext.empty(), producerHeaders)
        val producer = exporter.finishedSpanItems.first { it.kind == SpanKind.PRODUCER }

        val result = tracing.traceConsume("orders", producerHeaders, Flux.just("payload")).collectList().block()

        assertEquals(listOf("payload"), result)
        val consumer = exporter.finishedSpanItems.first { it.kind == SpanKind.CONSUMER }
        assertEquals(1, consumer.links.size)
        assertEquals(producer.spanContext.traceId, consumer.links[0].spanContext.traceId)
        assertEquals(producer.spanContext.spanId, consumer.links[0].spanContext.spanId)
    }
}
