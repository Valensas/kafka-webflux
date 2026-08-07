package com.valensas.common.kafka.webflux.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter
import org.apache.kafka.common.header.Headers
import reactor.core.publisher.Flux
import reactor.util.context.ContextView
import java.lang.reflect.Method
import reactor.util.context.Context as ReactorContext

class KafkaTracing(
    openTelemetry: OpenTelemetry
) {
    private val propagator = openTelemetry.propagators.textMapPropagator
    private val tracer = openTelemetry.getTracer(INSTRUMENTATION_NAME)

    fun produce(
        topic: String,
        contextView: ContextView,
        headers: Headers
    ) {
        val parent = getOpenTelemetryContext(contextView)
        val span =
            tracer
                .spanBuilder("$topic publish")
                .setParent(parent)
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination.name", topic)
                .setAttribute("messaging.operation", "publish")
                .startSpan()
        propagator.inject(parent.with(span), headers, SETTER)
        span.end()
    }

    fun <T : Any> traceConsume(
        topic: String,
        headers: Headers,
        flux: Flux<T>
    ): Flux<T> {
        val producerContext = propagator.extract(Context.current(), headers, GETTER)
        val span =
            tracer
                .spanBuilder("$topic process")
                .setNoParent()
                .addLink(Span.fromContext(producerContext).spanContext)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination.name", topic)
                .setAttribute("messaging.operation", "process")
                .startSpan()
        val otelContext = Context.root().with(span)

        return flux
            .contextWrite { reactorContext -> storeOpenTelemetryContext(reactorContext, otelContext) }
            .doOnError { error -> span.recordException(error) }
            .doFinally { span.end() }
    }

    companion object {
        const val OTEL_CONTEXT_KEY = "com.valensas.kafka-webflux.otel-context"

        private const val INSTRUMENTATION_NAME = "com.valensas.kafka-webflux"

        private const val BRIDGE = "io.opentelemetry.instrumentation.reactor.v3_1.ContextPropagationOperator"

        private val storeContextMethod: Method? =
            runCatching {
                Class.forName(BRIDGE).getMethod("storeOpenTelemetryContext", ReactorContext::class.java, Context::class.java)
            }.getOrNull()

        private val getContextMethod: Method? =
            runCatching {
                Class.forName(BRIDGE).getMethod("getOpenTelemetryContextFromContextView", ContextView::class.java, Context::class.java)
            }.getOrNull()

        private fun storeOpenTelemetryContext(
            reactorContext: ReactorContext,
            otelContext: Context
        ): ReactorContext {
            val bridged =
                storeContextMethod
                    ?.let { method ->
                        runCatching { method.invoke(null, reactorContext, otelContext) as ReactorContext }
                            .getOrDefault(reactorContext)
                    } ?: reactorContext
            return bridged.put(OTEL_CONTEXT_KEY, otelContext)
        }

        private fun getOpenTelemetryContext(contextView: ContextView): Context {
            val method = getContextMethod ?: return Context.current()
            return runCatching { method.invoke(null, contextView, Context.current()) as Context }
                .getOrDefault(Context.current())
        }

        private val GETTER =
            object : TextMapGetter<Headers> {
                override fun keys(carrier: Headers): Iterable<String> = carrier.map { it.key() }.distinct()

                override fun get(
                    carrier: Headers?,
                    key: String
                ): String? = carrier?.lastHeader(key)?.value()?.toString(Charsets.UTF_8)
            }

        private val SETTER =
            TextMapSetter<Headers> { carrier, key, value ->
                carrier?.remove(key)
                carrier?.add(key, value.toByteArray(Charsets.UTF_8))
            }
    }
}
