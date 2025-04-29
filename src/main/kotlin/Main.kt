import io.ktor.http.*
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val logger = LoggerFactory.getLogger("Main")

@Serializable
data class WordDefinition(val word: String, val definition: String)

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(CORS) {
            allowHost("localhost:5173")
            allowMethod(HttpMethod.Get)
            allowHeader(HttpHeaders.ContentType)
        }
        routing {
            get("/getWordDefinitions") {
                val startTime = call.request.queryParameters["startTime"]?.toIntOrNull()
                val endTime = call.request.queryParameters["endTime"]?.toIntOrNull()

                if (startTime == null || endTime == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid startTime or endTime")
                    return@get
                }

                // For now, don't require subtitle file as request param. Assume fixed name.
                // TODO singleton per filename
                val subtitleParser = SubtitleParser("MIB2-subtitles-pt-BR.vtt")

                // TODO fix
                val words = subtitleParser.getWords(startTime, endTime)
                val fakeDefinitions = mutableListOf<WordDefinition>()
                for (word in words) {
                    logger.info("Word: $word")
                    fakeDefinitions.add(WordDefinition(word, "Definition of $word"))
                }

                call.respond(HttpStatusCode.OK, Json.encodeToString(fakeDefinitions))
            }
        }
    }.start(wait = true)
}