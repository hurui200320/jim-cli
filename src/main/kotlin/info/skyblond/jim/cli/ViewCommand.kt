package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.prettyString
import org.jetbrains.exposed.sql.transactions.transaction

object ViewCommand : CliktCommand(
    name = "view",
    help = "View details of a given entry"
) {
    private val entryId: String by argument("entry_id")
        .check("Entry not found") { transaction { Entry.existsById(it.uppercase()) } }

    override fun run() {
        transaction {
            Entry.selectById(entryId.uppercase()) ?: error("Entry found but not found")
        }.prettyString().also { echo(it) }
    }

}
