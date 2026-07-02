package com.example.cgallery.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class MediaType {
    IMAGE, VIDEO, GIF
}

@Serializable
data class MediaItem(
    val id: Long,
    @Serializable(with = UriSerializer::class)
    val uri: Uri,
    val displayName: String,
    val bucketName: String,
    val relativePath: String,
    val type: MediaType,
    val duration: Long = 0L,
    val dateAdded: Long = 0L
)

object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parse(decoder.decodeString())
    }
}
