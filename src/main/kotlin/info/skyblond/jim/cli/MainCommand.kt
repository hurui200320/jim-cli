package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import info.skyblond.jim.core.db.Entries
import info.skyblond.jim.core.db.Metadatas
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SchemaUtils.withDataBaseLock
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object MainCommand : CliktCommand() {

    init {
        subcommands(
            TUICommand, // TUI
            ServerCommand, // HTTP API
            // resources, add, update, delete
            // with sub command entry or meta
            AddCommand,
            UpdateCommand,
            DeleteCommand,
            // general search and view
            SearchCommand, // by keyword
            BrowseCommand, // by parent
            ViewCommand, // by entry id
            // db sync
            // delete orphan metadata, set unreachable entries' parent to "L_UNREACHABLE"
            SyncCommand,
            // import and export json
            ExportCommand,
            ImportCommand,
        )
    }

    private val defaultDBFile =
        File(System.getProperty("user.home") + File.separator + ".jim/app.db")
    private val dbFile: File by option("--db").file(canBeDir = false)
        .default(defaultDBFile)
        .help("Database path. Default: $defaultDBFile")

    override fun run() {
        dbFile.parentFile?.mkdirs()
        Database.connect(
            "jdbc:sqlite:$dbFile",
            driver = "org.sqlite.JDBC",
        )
        transaction {
            withDataBaseLock {
                SchemaUtils.create(Entries, Metadatas)
            }
        }
    }
}
