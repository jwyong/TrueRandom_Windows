package com.truerandom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.truerandom.auth.SpotifyAuth
import com.truerandom.auth.SpotifyAuth.startSpotifyAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import jdk.internal.org.jline.utils.AttributedStringBuilder.append
import org.jetbrains.compose.resources.painterResource
import truerandomwindows.composeapp.generated.resources.Res
import truerandomwindows.composeapp.generated.resources.compose_multiplatform

const val CLIENT_ID = "e61d6a48cd14457c97e43850f03eb35c"
const val REDIRECT_URI = "http://127.0.0.1:8888/callback"
val AUTH_SCOPE = listOf(
    "user-read-email",
    "user-read-private",
    "streaming",
    "user-library-read",
    "user-modify-playback-state"
).joinToString(" ")

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                // 1. Start listening for the response
                listenForSpotifyCallback { code ->
                    println("Got the code: $code")
                    // 2. Now use Ktor to exchange this 'code' for an 'access_token'
                }

                // 2. Open the browser
                startSpotifyAuth(CLIENT_ID, REDIRECT_URI, AUTH_SCOPE)
            }) {
                Text("Connect Spotify")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}

// Listen to browser auth done callback (loopback)
fun listenForSpotifyCallback(onCodeReceived: (String) -> Unit) {
    // Port 8888 is correct. Ensure it's not blocked by a firewall.
    val server = embeddedServer(Netty, port = 8888, host = "127.0.0.1") {
        routing {
            get("/callback") {
                val code = call.parameters["code"]
                if (code != null) {
                    call.respondText("Login successful! Return to TrueRandom.")
                    onCodeReceived(code)
                }
            }
        }
    }
    server.start(wait = false)
}

// Get accessToken from temp auth code (~10 mins)
suspend fun exchangeCodeForToken(code: String): String? {
    val client = HttpClient(CIO)
    val verifier = SpotifyAuth.getVerifier() ?: return null

    val response: HttpResponse = client.post("https://accounts.spotify.com/api/token") {
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(
            FormDataContent(Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", "http://127.0.0.1:8888/callback")
                append("client_id", CLIENT_ID)
                append("code_verifier", verifier) // PKCE requirement
            })
        )
    }

    // This will return the JSON containing your access_token and refresh_token
    return response.bodyAsText()
}