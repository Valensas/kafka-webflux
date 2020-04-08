package com.valensas.common.kafka.webflux.util

import reactor.kafka.receiver.ReceiverOptions

interface ReceiverCustomizer {
    fun customize(receiverOptions: ReceiverOptions<String, Any>): ReceiverOptions<String, Any>
}
