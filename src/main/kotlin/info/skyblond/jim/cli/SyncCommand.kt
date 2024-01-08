package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import info.skyblond.jim.core.db.Entries
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Metas
import info.skyblond.jim.core.prettyString
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

object SyncCommand : CliktCommand(
    name = "sync",
    help = "Try to fix database and delete any orphan (unreachable) entries"
) {
    override fun run() {
        echo("Scanning... Please do not perform any operation")
        val queue = LinkedList<String?>()
        queue.add(null)
        val foundEntryId = mutableSetOf<String>()
        transaction {
            while (queue.isNotEmpty()) {
                val parent = queue.remove()
                Entry.selectAllByParentEntryId(parent).forEach { entry ->
                    queue.add(entry.entryId)
                    foundEntryId.add(entry.entryId)
                }
            }
        }

        // found all orphan entries
        val allOrphanEntryId = transaction {
            Entries
                .slice(Entries.entryId)
                .select { Entries.entryId notInList foundEntryId }
                .withDistinct(true)
                .orderBy(Entries.type to SortOrder.ASC, Entries.entryId to SortOrder.ASC)
                .map { it[Entries.entryId] }.toMutableSet()
        }
        if (allOrphanEntryId.isNotEmpty())
            echo("The following entries will be deleted:")
        allOrphanEntryId.forEach { entryId ->
            // try printing the entry to be deleted
            try {
                transaction {
                    echo(Entry.selectById(entryId)!!.prettyString("    "))
                }
            } catch (t: Throwable) {
                echoErr("Failed to fetch and print entry $entryId")
            }
        }

        if (allOrphanEntryId.isNotEmpty())
            echo("Fixing...")
        transaction {
            // delete orphan entries
            Entries
                .deleteWhere { Entries.entryId inList allOrphanEntryId }
            // fix parent ref
            Entries
                .update({ Entries.parentEntryId inList allOrphanEntryId }) {
                    it[Entries.parentEntryId] = null
                }
            // delete related metadata
            Metas.deleteWhere { Metas.entryId inList allOrphanEntryId }
        }

        echo("Removed ${allOrphanEntryId.size} orphan entry(s)")
    }

}
