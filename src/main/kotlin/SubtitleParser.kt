import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// TODO unit test with fake subtitle file text
class SubtitleParser(private val subtitleFile: String) {

    private val timeToWords: MutableMap<Int, Set<String>> = mutableMapOf()

    init {
        // open subtitle file
        val vttFilePath = Paths.get("src/main/resources/static/$subtitleFile")
        val lines = Files.readAllLines(vttFilePath)

        // subtitle timestamp format
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

        // words will be keyed by the start time
        var startTimeInSeconds: Int? = null

        for (line in lines) {
            when {
                line.isBlank() -> continue // empty line
                line.toIntOrNull() != null -> continue // row number only
                line.contains("-->") -> { // time range
                    // e.g. 00:00:04.100 --> 00:00:06.000
                    val times = line.split(" --> ")
                    startTimeInSeconds = LocalTime.parse(times[0].trim(), formatter).toSecondOfDay()
                }
                else -> { // valid line
                    if (startTimeInSeconds != null) {
                        recordWordsAtTime(startTimeInSeconds, line)
                    }
                }
            }
        }
    }

    private fun recordWordsAtTime(startTimeInSeconds: Int, text: String) {
        timeToWords[startTimeInSeconds] = text.toString().trim().split(" ").toSet()
    }

    fun getWords(startTime: Int, endTime: Int): List<String> {
        val result = mutableListOf<String>()
        // TODO better to loop startTime to endTime
        for ((time, words) in timeToWords) {
            if (time in startTime..endTime) {
                result.addAll(words)
            }
        }
        return result
    }
}