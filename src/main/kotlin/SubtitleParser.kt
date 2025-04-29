import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// TODO unit test with fake subtitle file text
class SubtitleParser(private val subtitleFile: String) {

    private val timeToWords: MutableMap<Int, List<String>> = mutableMapOf()

    init {
        val vttFilePath = Paths.get("src/main/resources/static/$subtitleFile")
        val lines = Files.readAllLines(vttFilePath)
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

        var currentText = StringBuilder()
        var startTimeInSeconds: Int? = null

        for (line in lines) {
            if (line.contains("-->")) {
                if (startTimeInSeconds != null && currentText.isNotEmpty()) {
                    logger.info("Adding words at $startTimeInSeconds: $currentText")
                    timeToWords[startTimeInSeconds] = currentText.toString().trim().split(" ")
                    currentText = StringBuilder()
                }

                val times = line.split(" --> ")
                startTimeInSeconds = LocalTime.parse(times[0].trim(), formatter).toSecondOfDay()
            } else if (line.isNotBlank()) {
                // line number only, skip
                if (line.toIntOrNull() != null) {
                    continue
                }
                currentText.append(line).append(" ")
            }
        }

        if (startTimeInSeconds != null && currentText.isNotEmpty()) {
            logger.info("All lines done. Adding words from: $currentText")
            timeToWords[startTimeInSeconds] = currentText.toString().trim().split(" ")
        }
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