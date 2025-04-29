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

fun parseVttFile(startTime: Int, endTime: Int): List<String> {
    val vttFilePath = Paths.get("src/main/resources/static/MIB2-subtitles-pt-BR.vtt")
    val lines = Files.readAllLines(vttFilePath)
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    val result = mutableListOf<String>()

    var currentText = StringBuilder()
    var withinTimeRange = false

    for (line in lines) {
        if (line.contains("-->")) {
            val times = line.split(" --> ")
            val start = LocalTime.parse(times[0].trim(), formatter).toSecondOfDay()
            val end = LocalTime.parse(times[1].trim(), formatter).toSecondOfDay()

            withinTimeRange = start in startTime..endTime || end in startTime..endTime
            if (!withinTimeRange && currentText.isNotEmpty()) {
                result.add(currentText.toString().trim())
                currentText = StringBuilder()
            }
        } else if (withinTimeRange && line.isNotBlank()) {
            logger.info("Read subtitle line: ${line}")
            currentText.append(line).append(" ")
        }
    }

    if (currentText.isNotEmpty()) {
        result.add(currentText.toString().trim())
    }

    // TODO proper word handling
    if (!result.isEmpty()) {
        val words = result[0].split(" ")
        return words
    }

    return result
}

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

                // TODO fix
                val words = parseVttFile(startTime ?: 0, endTime ?: 0)
                val fakeDefinitions = mutableListOf<WordDefinition>()
                for (word in words) {
                    logger.info("Word: $word")
                    fakeDefinitions.add(WordDefinition(word, "Definition of $word"))
                }

//                // Fake dictionary and definitions
//                val fakeDefinitions = listOf(
//                    WordDefinition("hello", "A greeting"),
//                    WordDefinition("world", "The earth, together with all of its countries and peoples")
//                )

                call.respond(HttpStatusCode.OK, Json.encodeToString(fakeDefinitions))
            }
        }
    }.start(wait = true)
}