package com.github.tmarsteel.kskspider.authentication

data class Credentials(
    val host: String,
    val username: String,
    val pin: String
) {
    init {
        if (!HOST_REGEX.matches(host)) {
            throw IllegalArgumentException("The host should only be the TLD of the Bank, e.g. kskbb.de")
        }
    }
}

val HOST_REGEX = Regex("\\w+\\.\\w{2}")