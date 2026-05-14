package dev.brahmkshatriya.echo.common.models

actual class PlatformFile(val path: String) {
    override fun toString(): String = path
}

actual abstract class PlatformInputStream
