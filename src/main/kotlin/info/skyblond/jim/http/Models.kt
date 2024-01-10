package info.skyblond.jim.http

import com.fasterxml.jackson.annotation.JsonProperty
import info.skyblond.jim.core.db.Entry
import info.skyblond.jim.core.db.Meta
import info.skyblond.jim.http.MetaModel.Companion.toModel
import org.jetbrains.exposed.sql.transactions.transaction

data class JimRequest(
    @JsonProperty("request_id")
    val requestId: String,
    @JsonProperty("cmd")
    val command: String,
    @JsonProperty("params")
    val params: List<*> = emptyList<Any>()
)

data class JimResponse(
    @JsonProperty("request_id")
    val requestId: String,
    @JsonProperty("err")
    val error: String?,
    @JsonProperty("result")
    val result: Any?
)

fun respErr(requestId: String, message: String) =
    JimResponse(requestId, message, null)

fun respOk(requestId: String, result: Any?) =
    JimResponse(requestId, null, result)

data class MetaModel(
    @JsonProperty("meta_name")
    val name: String,
    @JsonProperty("meta_type")
    val metaType: String,
    @JsonProperty("meta_need_value")
    val needValue: Boolean,
    @JsonProperty("meta_value")
    val value: String
) {
    companion object {
        fun Meta.toModel(): MetaModel = MetaModel(
            name = this.name,
            metaType = this.type.name,
            needValue = this.type.needValue,
            value = if (this.type.needValue) this.value else ""
        )
    }
}

data class EntryModel(
    @JsonProperty("entry_id")
    val entryId: String,
    @JsonProperty("entry_type")
    val entryType: String,
    @JsonProperty("entry_parent_id")
    val parentId: String?,
    @JsonProperty("entry_children_count")
    val childrenCount: Long,
    @JsonProperty("entry_name")
    val name: String,
    @JsonProperty("entry_note")
    val note: String,
    @JsonProperty("entry_meta_list")
    val metas: List<MetaModel>
) {
    companion object {
        fun Entry.toModel(): EntryModel = EntryModel(
            entryId = this@toModel.entryId,
            entryType = this@toModel.type.name,
            parentId = this@toModel.parentEntryId,
            name = this@toModel.name,
            note = this@toModel.note,
            childrenCount = transaction { Entry.countByParentEntryId(this@toModel.entryId) },
            metas = transaction { this@toModel.listMetadata() }.map { it.toModel() }
        )
    }
}
