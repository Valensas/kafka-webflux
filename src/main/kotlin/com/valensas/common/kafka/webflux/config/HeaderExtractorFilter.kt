package com.valensas.common.kafka.webflux.config

import com.valensas.common.kafka.webflux.properties.HeaderPropagationProperties
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context

class HeaderExtractorFilter(
    private val headerPropagationProperties: HeaderPropagationProperties
) : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void> {
        val propagatedHeaders = mutableMapOf<String, String>()

        headerPropagationProperties.headers.forEach { header ->
            exchange.request.headers[header]?.let { values ->
                propagatedHeaders[header] = values.first().toString()
            }
        }

        return chain.filter(
            exchange
        ).contextWrite(Context.of(headerPropagationProperties.contextKey, propagatedHeaders))
    }
}
