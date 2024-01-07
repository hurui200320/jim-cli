package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.outputStream
import info.skyblond.jim.core.createPrettyJsonWriter
import info.skyblond.jim.core.exportDataToJson

object ExportCommand : CliktCommand(
    name = "export",
    help = "Export data into a json file, won't include orphan entities"
) {
    private val target by argument("target").outputStream(
        createIfNotExist = true, truncateExisting = true
    )

    override fun run() {
        createPrettyJsonWriter(target.writer()).use {
            exportDataToJson(it)
            it.flush()
        }
    }

}
