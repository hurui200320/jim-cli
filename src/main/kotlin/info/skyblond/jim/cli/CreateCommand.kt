package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import info.skyblond.jim.core.db.Entries
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.core.db.Metas
import info.skyblond.jim.core.prettyString
import org.jetbrains.exposed.sql.transactions.transaction

object CreateCommand : CliktCommand(
    name = "create",
    help = "Create entry or meta"
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
        help = "Create entry"
    ) {
        private val _entryId: String by option("--entry-id")
            .required()
            .help("The entry id")
            .check("Entry already exists") {
                transaction { !Entry.existsById(it.uppercase()) }
            }

        private val entryId: String
            get() = _entryId.uppercase()

        private val entryType: String? by option(            "--type"        )
            .choice(*Entries.Type.entries.map { it.name }.toTypedArray(), ignoreCase = true)
            .help("The entry type, will automatically inferred from entry ID if you follow the naming rule")

        private val parentId: String? by option("--parent-id")
            .help("The parent entry id, must exists if not null")
            .check("Parent entry not found") {
                transaction { Entry.existsById(it.uppercase()) }
            }

        private val name: String by option("--name")
            .required()
            .help("The name of entry")
            .check("Name must be single line") {
                it.lines().count() == 1
            }

        private val note: String by option("--note")
            .default("")
            .help("Additional note for this entry (single line)")

        private val multilineNote by option("--multiline-note")
            .flag()
            .help("Read multiple lines of note from stdin, omit `--note`. " +
                    "Use ctrl+Z (Windows) or ctrl+D on a new line to send EOF")

        override fun run() {
            val actualNote = if (multilineNote) {
                echo("Type your note: ", trailingNewline = false)
                val sb = StringBuilder()
                var buffer: String
                do {
                    buffer = readlnOrNull() ?: break
                    sb.appendLine(buffer)
                } while (true)
                sb.toString().trim()
            } else note

            transaction {
                val e = Entry(
                    entryId = entryId,
                    type = entryType?.let { Entries.Type.valueOf(it) }
                        ?: when (entryId.first()) {
                            'L' -> Entries.Type.LOCATION
                            'B' -> Entries.Type.BOX
                            'I' -> Entries.Type.ITEM
                            else -> throw IllegalArgumentException("Cannot infer type from entry id: $entryId")
                        },
                    parentEntryId = parentId,
                    name = name,
                    note = actualNote
                )
                e.insert()
                e
            }.also {
                echo(it.prettyString())
            }
        }
    }

    private object MetaCommand : CliktCommand(
        name = "meta",
        help = "Create meta, need a existing entry"
    ) {
        private val _entryId: String by option("--entry-id")
            .required()
            .help("The entry id")
            .check("Entry not found") {
                transaction { Entry.existsById(it.uppercase()) }
            }

        private val entryId: String
            get() = _entryId.uppercase()

        private val name: String by option("--name")
            .required()
            .help("The name of entry")
            .check("Meta duplicate on name") {
                transaction { !Meta.existsByIdAndName(entryId, it) }
            }

        private val metaType: String by option("--type")
            .choice(*Metas.Type.entries.map { it.name }.toTypedArray(), ignoreCase = true)
            .required()
            .help("The meta type")

        private val value: String by option("--value")
            .default("")
            .help("The value of meta, optional depends on type")

        private val multilineValue by option("--multiline-value")
            .flag()
            .help("Read multiple lines of value from stdin, omit `--value`. " +
                    "Use ctrl+Z (Windows) or ctrl+D on a new line to send EOF")


        override fun run() {
            val actualValue = if (multilineValue) {
                echo("Type your value: ", trailingNewline = false)
                val sb = StringBuilder()
                var buffer: String
                do {
                    buffer = readlnOrNull() ?: break
                    sb.appendLine(buffer)
                } while (true)
                sb.toString().trim()
            } else value
            transaction {
                Meta(
                    entryId = entryId,
                    type = Metas.Type.valueOf(metaType),
                    name = name,
                    value = actualValue
                ).also { it.insert() }
            }.also {
                echo("ON ${entryId}:")
                echo(it.prettyString("\t"))
            }
        }
    }
}
