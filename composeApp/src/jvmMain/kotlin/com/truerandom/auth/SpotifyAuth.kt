package com.truerandom.auth

import java.awt.Desktop
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import kotlin.random.Random

object SpotifyAuth {
    private var currentCodeVerifier: String? = null

    fun startSpotifyAuth(clientId: String, redirectUri: String, scopes: String) {
        // 1. Generate PKCE values
        val verifier = generateCodeVerifier()
        currentCodeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)

        // 2. Build the Auth URL with PKCE parameters
        val authUrl = "https://accounts.spotify.com/authorize?" +
                "client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=$redirectUri" +
                "&scope=$scopes" +
                "&code_challenge_method=S256" +
                "&code_challenge=$challenge"

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(authUrl))
        }
    }

    fun getVerifier() = currentCodeVerifier

    private fun generateCodeVerifier(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + '-' + '.' + '_' + '~'
        return (1..64).map { allowedChars.random() }.joinToString("")
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}