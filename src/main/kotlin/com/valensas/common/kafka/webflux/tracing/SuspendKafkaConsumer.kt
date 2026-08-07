package com.valensas.common.kafka.webflux.tracing

import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.reactivestreams.Publisher
import kotlin.coroutines.CoroutineContext

/**
 * Bridges a suspend handler into the Publisher-based consumer contract while keeping the
 * OpenTelemetry context stored by [KafkaTracing.traceConsume] current on every thread the
 * coroutine resumes on, so Span.current() and MDC-based log correlation work inside the handler.
 */
fun <T> suspendConsumer(block: suspend CoroutineScope.(T) -> Unit): (T) -> Publisher<Unit> = { payload ->
    mono {
        val otelContext =
            coroutineContext[ReactorContext]
                ?.context
                ?.getOrDefault(KafkaTracing.OTEL_CONTEXT_KEY, null as Context?)

        if (otelContext == null) {
            block(payload)
        } else {
            withContext(OpenTelemetryContextElement(otelContext)) { block(payload) }
        }
    }
}

private class OpenTelemetryContextElement(
    private val otelContext: Context
) : ThreadContextElement<Scope> {
    companion object Key : CoroutineContext.Key<OpenTelemetryContextElement>

    override val key: CoroutineContext.Key<OpenTelemetryContextElement> = Key

    override fun updateThreadContext(context: CoroutineContext): Scope = otelContext.makeCurrent()

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: Scope
    ) {
        oldState.close()
    }
}
