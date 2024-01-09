package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.core.db.Metas
import info.skyblond.jim.core.prettyString
import org.jetbrains.exposed.sql.transactions.transaction

object UpdateCommand : CliktCommand(
    name = "update",
    help = "Update the info of a given entry or meta"
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
        help = "Update the info of a given entry"
    ) {
        private val _entryId: String by argument(name = "entry-id")
            .help("The entry id you want to update")
            .check("Entry not found") {
                transaction { Entry.existsById(it.uppercase()) }
            }

        private val entryId: String
            get() = _entryId.uppercase()

        private val parentId: String? by option("--parent-id")
            .help("The new parent entry id, must exists if not `null`")
            .check("Parent entry not found") {
                it == "null" || transaction { Entry.existsById(it.uppercase()) }
            }

        private val name: String? by option("--name")
            .help("The new name of entry")

        private val note: String? by option("--note")
            .help("New note for this entry (single line)")

        private val multilineNote by option("--multiline-note")
            .flag()
            .help(
                "Read multiple lines of note from stdin, omit `--note`. " +
                        "Use ctrl+Z (Windows) or ctrl+D on a new line to send EOF"
            )

        override fun run() = displayWhenError {
            val actualNote = if (multilineNote) {
                echo("New note: ", trailingNewline = false)
                val sb = StringBuilder()
                var buffer: String
                do {
                    buffer = readlnOrNull() ?: break
                    sb.appendLine(buffer)
                } while (true)
                sb.toString().trim()
            } else note

            transaction {
                Entry.selectById(entryId)?.apply {
                    this@EntryCommand.name?.let { this.name = it }
                    this@EntryCommand.parentId?.let {
                        this.parentEntryId = if (it == "null") null else it.uppercase()
                    }
                    actualNote?.let { this.note = it }
                    update()
                } ?: error("Entry not found")
            }.also {
                echo(it.prettyString())
            }
        }
    }

    private object MetaCommand : CliktCommand(
        name = "meta",
        help = "Update the info of a given meta"
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

        private val metaType: String? by option("--type")
            .choice(*Metas.Type.entries.map { it.name }.toTypedArray(), ignoreCase = true)
            .help("The new meta type")

        private val value: String? by option("--value")
            .help("The new value of metadata, optional depends on type")

        private val multilineValue by option("--multiline-value")
            .flag()
            .help(
                "Read multiple lines of value from stdin, omit `--value`. " +
                        "Use ctrl+Z (Windows) or ctrl+D on a new line to send EOF"
            )

        override fun run() = displayWhenError {
            val actualValue = if (multilineValue) {
                echo("New value: ", trailingNewline = false)
                val sb = StringBuilder()
                var buffer: String
                do {
                    buffer = readlnOrNull() ?: break
                    sb.appendLine(buffer)
                } while (true)
                sb.toString().trim()
            } else value

            transaction {
                Meta.selectByIdAndName(entryId, name)?.apply {
                    this@MetaCommand.metaType?.let { this.type = Metas.Type.valueOf(it) }
                    if (this.type.needValue)
                        actualValue?.let { this.value = it }
                    update()
                } ?: error("Metadata not found")
            }.also {
                echo("ON ${entryId}:")
                echo(it.prettyString("\t"))
            }
        }
    }

}
