package info.skyblond.jim.core

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import info.skyblond.jim.core.db.Entries
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.core.db.Metas
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Writer
import java.util.*
import java.util.concurrent.atomic.AtomicLong

fun createPrettyJsonWriter(writer: Writer) = JsonWriter(writer).apply {
    setIndent("  ")
    serializeNulls = true
}

data class ExportStatistic(
    val entryImportCounter: Long,
    val metadataImportCounter: Long,
)

fun exportDataToJson(jsonWriter: JsonWriter, db: Database? = null): ExportStatistic {
    val entryCounter = AtomicLong(0L)
    val metadataCounter = AtomicLong(0L)
    // the file is a big array
    jsonWriter.beginArray()
    // each entry is an object, containing their metadata
    // start from null parent, aka root, then BFS
    // otherwise we might reference parent that didn't exist yet
    val queue = LinkedList<String?>()
    queue.add(null)
    transaction(db) {
        while (queue.isNotEmpty()) {
            val parent = queue.remove()
            Entry.selectAllByParentEntryId(parent).forEach { entry ->
                queue.add(entry.entryId)
                // entry
                jsonWriter.beginObject()

                // entry's fields
                jsonWriter.name("entryId")
                jsonWriter.value(entry.entryId)
                jsonWriter.name("type")
                jsonWriter.value(entry.type.name)
                jsonWriter.name("parentEntryId")
                jsonWriter.value(entry.parentEntryId)
                jsonWriter.name("name")
                jsonWriter.value(entry.name)
                jsonWriter.name("note")
                jsonWriter.value(entry.note)

                // metadata related to this
                jsonWriter.name("metadata")
                jsonWriter.beginArray()
                entry.listMetadata().forEach { metadata ->
                    jsonWriter.beginObject()
                    jsonWriter.name("name")
                    jsonWriter.value(metadata.name)
                    jsonWriter.name("type")
                    jsonWriter.value(metadata.type.name)
                    jsonWriter.name("value")
                    jsonWriter.value(metadata.value)
                    jsonWriter.endObject()
                    metadataCounter.incrementAndGet()
                }
                jsonWriter.endArray()

                jsonWriter.endObject()
                entryCounter.incrementAndGet()
            }
        }
    }
    // the end of file
    jsonWriter.endArray()
    return ExportStatistic(entryCounter.get(), metadataCounter.get())
}

data class ImportStatistic(
    val entryImportCounter: Long,
    val entryOverwriteCounter: Long,
    val entrySkipCounter: Long,
    val metadataImportCounter: Long,
    val metadataOverwriteCounter: Long,
    val metadataSkipCounter: Long
)

private fun parseMetadataMap(jsonReader: JsonReader): Map<String, Any> {
    jsonReader.beginObject()
    val metadataMap = mutableMapOf<String, Any>()
    while (jsonReader.hasNext() && jsonReader.peek() == JsonToken.NAME) {
        when (val innerName = jsonReader.nextName()) {
            "name" -> metadataMap[innerName] = jsonReader.nextString()
            "type" -> metadataMap[innerName] =
                jsonReader.nextString().let { Metas.Type.valueOf(it) }

            "value" -> metadataMap[innerName] = jsonReader.nextString()
        }
    }
    jsonReader.endObject()
    return metadataMap
}

private fun parseEntryMap(jsonReader: JsonReader): Pair<Map<String, Any>, List<Map<String, Any>>> {
    jsonReader.beginObject()
    val entryMap = mutableMapOf<String, Any>()
    val metadataList = mutableListOf<Map<String, Any>>()
    while (jsonReader.hasNext() && jsonReader.peek() == JsonToken.NAME) {
        when (val name = jsonReader.nextName()) {
            "entryId" -> entryMap[name] = jsonReader.nextString()
            "type" -> entryMap[name] = jsonReader.nextString().let { Entries.Type.valueOf(it) }
            "parentEntryId" -> if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull()
            } else {
                entryMap[name] = jsonReader.nextString()
            }

            "name" -> entryMap[name] = jsonReader.nextString()
            "note" -> entryMap[name] = jsonReader.nextString()
            "metadata" -> { // metadata
                jsonReader.beginArray()
                while (jsonReader.hasNext() && jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                    metadataList.add(parseMetadataMap(jsonReader))
                }
                jsonReader.endArray()
            }
        }
    }
    jsonReader.endObject()
    return entryMap to metadataList
}


fun importDataFromJson(
    jsonReader: JsonReader,
    forceOverwrite: Boolean,
    db: Database? = null,
    // here we use lambda to skip string eval if we don't print them
    verboseOutput: (() -> String) -> Unit
): ImportStatistic {
    val entryImportCounter = AtomicLong()
    val entryOverwriteCounter = AtomicLong(0L)
    val entrySkipCounter = AtomicLong(0L)
    val metadataImportCounter = AtomicLong(0L)
    val metadataOverwriteCounter = AtomicLong(0L)
    val metadataSkipCounter = AtomicLong(0L)

    transaction(db) {
        jsonReader.beginArray()
        while (jsonReader.hasNext() && jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
            val (entryMap, metadataList) = parseEntryMap(jsonReader)
            // insert entry first
            val entry = Entry(
                entryId = entryMap["entryId"] as String,
                type = entryMap["type"] as Entries.Type,
                parentEntryId = entryMap["parentEntryId"] as String?,
                name = entryMap["name"] as String,
                note = entryMap["note"] as String,
            )

            if (Entry.existsById(entry.entryId)) {
                if (forceOverwrite) { // update
                    entry.insertOrUpdate()
                    entryOverwriteCounter.incrementAndGet()
                    verboseOutput { "Overwriting entry ${entry.entryId} (${entry.type})" }
                } else { // skip
                    entrySkipCounter.incrementAndGet()
                    verboseOutput { "Skipping entry ${entry.entryId} (${entry.type})" }
                }
            } else { // new obj
                entry.insert()
            }
            entryImportCounter.incrementAndGet()

            // then metadata
            for (metadataMap in metadataList) {
                val meta = Meta(
                    entryId = entry.entryId,
                    name = metadataMap["name"] as String,
                    type = metadataMap["type"] as Metas.Type,
                    value = metadataMap["value"] as String,
                )

                if (Meta.existsByIdAndName(meta.entryId, meta.name)) {
                    if (forceOverwrite) { // update
                        meta.insertOrUpdate()
                        metadataOverwriteCounter.incrementAndGet()
                        verboseOutput { "Overwriting metadata ${meta.name} of ${meta.entryId} (${meta.type})" }
                    } else { // skip
                        metadataSkipCounter.incrementAndGet()
                        verboseOutput { "Skipping metadata ${meta.name} of ${meta.entryId} (${meta.type})" }
                    }
                } else { // new obj
                    meta.insert()
                }
                metadataImportCounter.incrementAndGet()
            }
        }
        jsonReader.endArray()
    }

    return ImportStatistic(
        entryImportCounter = entryImportCounter.get(),
        entryOverwriteCounter = entryOverwriteCounter.get(),
        entrySkipCounter = entrySkipCounter.get(),
        metadataImportCounter = metadataImportCounter.get(),
        metadataOverwriteCounter = metadataOverwriteCounter.get(),
        metadataSkipCounter = metadataSkipCounter.get()
    )
}
