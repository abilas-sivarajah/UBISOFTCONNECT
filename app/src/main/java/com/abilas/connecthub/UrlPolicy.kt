package com.abilas.connecthub

import java.net.URI
import java.util.Locale

/** Decides whether a navigation stays in the app's web view or goes to another app. */
object UrlPolicy {

    enum class Target { IN_APP, EXTERNAL }

    // Schemes the web view handles itself.
    private val WEB_SCHEMES = setOf("http", "https", "about", "data", "blob", "javascript")

    // Sites that have their own apps and are never part of a Ubisoft sign-in or
    // checkout flow. Everything else stays in the app, because sign-in (Steam,
    // PlayStation, Xbox, ...) and payment (PayPal, 3-D Secure) pages have to finish
    // inside the same web view for the Ubisoft session to end up here.
    private val EXTERNAL_HOSTS = setOf(
        "youtube.com", "youtu.be", "twitter.com", "x.com", "instagram.com", "tiktok.com",
        "discord.com", "discord.gg", "reddit.com", "linkedin.com", "threads.net",
        "play.google.com", "apps.apple.com",
    )

    fun targetFor(url: String): Target {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase(Locale.ROOT)
        if (scheme !in WEB_SCHEMES) return Target.EXTERNAL
        if (scheme != "http" && scheme != "https") return Target.IN_APP
        val host = hostOf(url) ?: return Target.IN_APP
        return if (EXTERNAL_HOSTS.any { host == it || host.endsWith(".$it") }) Target.EXTERNAL else Target.IN_APP
    }

    private fun hostOf(url: String): String? =
        try {
            URI(url).host?.lowercase(Locale.ROOT)?.removeSuffix(".")
        } catch (_: Exception) {
            null
        }
}
