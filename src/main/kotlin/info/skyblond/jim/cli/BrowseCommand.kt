package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Metas
import info.skyblond.jim.core.prettyString
import org.jetbrains.exposed.sql.transactions.transaction

object BrowseCommand : CliktCommand(
    name = "browse",
    help = "Browse all entries under a given parent id"
) {
    private val parentId: String? by argument("parent-id")
        .optional()
        .help("The id of the entry you want to delete")
        .check("Parent entry not found") {
            transaction { Entry.existsById(it.uppercase()) }
        }

    private val displayAll by option("--all", "-a")
        .flag()
        .help("Display all details for each entry, just like `view` command")

    override fun run() = displayWhenError {
        transaction {
            Entry.selectAllByParentEntryId(parentId?.uppercase()).forEach { e ->
                if (displayAll) {
                    echo(e.prettyString())
                } else {
                    val sb = StringBuilder()
                    sb.append("${e.type}: ${e.entryId}").appendLine()
                    Entry.countByParentEntryId(e.entryId).let {
                        if (it != 0L) sb.append("\tChildren count: $it").appendLine()
                    }
                    sb.append("\tName: ${e.name}").appendLine()
                    val note = e.note.lines()
                    sb.append("\tNote: ").append(note.first())
                    if (note.size > 1) sb.append("...")
                    sb.appendLine()
                    val metas = e.listMetadata()
                    if (metas.isNotEmpty()) {
                        sb.append("\tMetadata count: ${metas.size}").appendLine()
                        sb.append("\tTags: ")
                        metas.filter { it.type == Metas.Type.TAG }
                            .forEachIndexed { index, t ->
                                if (index != 0) sb.append("\t    | ")
                                sb.append(t.name).appendLine()
                            }
                    }
                    echo(sb.toString())
                }
            }
        }
    }

}
