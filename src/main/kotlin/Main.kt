import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WordDefinition(val word: String, val definition: String)

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/getWordDefinitions") {
                val startTime = call.request.queryParameters["startTime"]?.toIntOrNull()
                val endTime = call.request.queryParameters["endTime"]?.toIntOrNull()

                if (startTime == null || endTime == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid startTime or endTime")
                    return@get
                }

                // Fake dictionary and definitions
                val fakeDefinitions = listOf(
                    WordDefinition("hello", "A greeting"),
                    WordDefinition("world", "The earth, together with all of its countries and peoples")
                )

                call.respond(HttpStatusCode.OK, Json.encodeToString(fakeDefinitions))
            }
        }
    }.start(wait = true)
}