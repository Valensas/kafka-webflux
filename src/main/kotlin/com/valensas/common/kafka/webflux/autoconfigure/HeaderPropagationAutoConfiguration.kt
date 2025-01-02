package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.config.HeaderExtractorFilter
import com.valensas.common.kafka.webflux.properties.HeaderPropagationProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono

@Configuration
@EnableConfigurationProperties(HeaderPropagationProperties::class)
@ConditionalOnProperty(
    prefix = "spring.kafka.propagation",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class HeaderPropagationAutoConfiguration() {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun headerPropagationFilter(headerPropagationProperties: HeaderPropagationProperties): WebFilter {
        return HeaderExtractorFilter(headerPropagationProperties)
    }

    @Bean
    fun headerPropagationExchangeFilter(headerPropagationProperties: HeaderPropagationProperties): ExchangeFilterFunction {
        return ExchangeFilterFunction { clientRequest: ClientRequest, next: ExchangeFunction ->
            Mono.deferContextual { context ->
                val newRequestBuilder = ClientRequest.from(clientRequest)
                headerPropagationProperties.headers.forEach { header ->
                    context.getOrDefault(headerPropagationProperties.contextKey, emptyMap<String, String>()).let { headers ->
                        headers?.get(header)?.let { headerValue ->
                            newRequestBuilder.header(header, headerValue)
                        }
                    }
                }
                next.exchange(newRequestBuilder.build())
            }
        }
    }
}
