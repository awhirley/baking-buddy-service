package com.bakingbuddy.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

sealed class PatchField<out T> {
    object Absent : PatchField<Nothing>()
    data class Present<T>(val value: T?) : PatchField<T>()
}

sealed class PatchFieldNonNull<out T> {
    object Absent : PatchFieldNonNull<Nothing>()
    data class Present<T>(val value: T) : PatchFieldNonNull<T>()
}

@OptIn(ExperimentalSerializationApi::class)
class PatchFieldSerializer<T>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<PatchField<T>> {

    override val descriptor: SerialDescriptor = valueSerializer.descriptor.nullable

    override fun deserialize(decoder: Decoder): PatchField<T> {
        return if (decoder.decodeNotNullMark()) {
            PatchField.Present(decoder.decodeSerializableValue(valueSerializer))
        } else {
            decoder.decodeNull()
            PatchField.Present(null)
        }
    }

    override fun serialize(encoder: Encoder, value: PatchField<T>) {
        when (value) {
            is PatchField.Absent -> encoder.encodeNull()
            is PatchField.Present ->
                if (value.value == null) {
                    encoder.encodeNull()
                } else {
                    encoder.encodeSerializableValue(valueSerializer, value.value)
                }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
class PatchFieldNonNullSerializer<T>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<PatchFieldNonNull<T>> {

    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun deserialize(decoder: Decoder): PatchFieldNonNull<T> {
        if (decoder.decodeNotNullMark()) {
            return PatchFieldNonNull.Present(decoder.decodeSerializableValue(valueSerializer))
        }
        decoder.decodeNull()
        throw SerializationException("This field cannot be set to null")
    }

    override fun serialize(encoder: Encoder, value: PatchFieldNonNull<T>) {
        when (value) {
            is PatchFieldNonNull.Absent ->
                throw SerializationException("Cannot serialize Absent — this type is for request payloads only")
            is PatchFieldNonNull.Present -> encoder.encodeSerializableValue(valueSerializer, value.value)
        }
    }
}