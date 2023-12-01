package com.valensas.common.kafka.webflux.serializer

import com.google.protobuf.MessageLite
import org.apache.kafka.common.serialization.Serializer

class ProtobufSerializer : Serializer<MessageLite> {
    override fun serialize(
        topic: String?,
        data: MessageLite
    ): ByteArray = data.toByteArray()
}
