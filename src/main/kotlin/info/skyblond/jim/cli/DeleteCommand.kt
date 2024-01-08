package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.help
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.core.prettyString
import org.jetbrains.exposed.sql.transactions.transaction

object DeleteCommand : CliktCommand(
    name = "delete",
    help = "Delete an entry or metadata"
) {
    init {
        subcommands(
            EntryCommand,
            MetaCommand
        )
    }

    override fun run() {
    }

    private object EntryCommand : CliktCommand(
        name = "entry",
        help = "Delete entry"
    ) {
        private val entryId: String by argument("entry-id")
            .help("The id of the entry you want to delete")
            .check("Entry not found") {
                transaction { Entry.existsById(it.uppercase()) }
            }

        override fun run() = displayWhenError {
            transaction {
                Entry.selectById(entryId.uppercase())?.also {
                    echo("Deleted: ")
                    echo(it.prettyString("\t"))
                    it.delete()
                } ?: error("Entry ${entryId.uppercase()} not found")
            }
        }
    }

    private object MetaCommand : CliktCommand(
        name = "meta",
        help = "Delete meta"
    ) {
        private val _entryId: String by argument("entry-id")
            .help("The entry id")
            .check("Entry not found") {
                transaction { Entry.existsById(it.uppercase()) }
            }

        private val entryId: String
            get() = _entryId.uppercase()

        private val name: String by argument("meta-name")
            .help("The name of metadata")
            .check("Meta not found") {
                transaction { Meta.existsByIdAndName(entryId, it) }
            }

        override fun run() = displayWhenError {
            transaction {
                Meta.selectByIdAndName(entryId, name)?.also {
                    echo("Deleted from ${entryId}:")
                    echo(it.prettyString("\t"))
                    it.delete()
                } ?: error("Entry $entryId don't have meta called $name")
            }
        }
    }
}
