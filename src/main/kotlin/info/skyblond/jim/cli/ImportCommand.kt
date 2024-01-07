package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.inputStream
import com.google.gson.stream.JsonReader
import info.skyblond.jim.core.importDataFromJson

object ImportCommand : CliktCommand(
    name = "import",
    help = "Import data from json file."
) {
    private val forceOverwrite by option("-f", "--force").flag()
        .help("Overwrite/update existing record, might produce orphan entities")

    private val verbose by option("-v", "--verbose").flag()
        .help("Print what is skipped or overwrote")

    private val target by argument("target").inputStream()

    override fun run() {
        val reader = target.reader()
        val jsonReader = JsonReader(reader)

        val result = importDataFromJson(jsonReader, forceOverwrite) {
            if (verbose) echo(it())
        }

        reader.close()
        echo("Imported ${result.entryImportCounter} entry(s)")
        result.entrySkipCounter.let { if (it > 0) echo("Skipped $it entry(s)") }
        result.entryOverwriteCounter.let { if (it > 0) echo("Overwrote $it entry(s)") }
        echo("Imported ${result.metadataImportCounter} metadata(s)")
        result.metadataSkipCounter.let { if (it > 0) echo("Skipped $it metadata(s)") }
        result.metadataOverwriteCounter.let { if (it > 0) echo("Overwrote $it metadata(s)") }
    }
}
