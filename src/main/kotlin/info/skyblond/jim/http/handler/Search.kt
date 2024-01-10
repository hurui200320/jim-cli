package info.skyblond.jim.http.handler

import info.skyblond.jim.core.searchKeywords
import info.skyblond.jim.http.castParam

/**
 * Params: [keyword: String...]
 * @see [info.skyblond.jim.cli.SearchCommand].
 * */
fun handleSearch(params: List<*>): List<String> {
    val keywords = params.indices
        .map { params.castParam(it, String::class) }
        .filter { it.isNotBlank() }

    return if (keywords.isNotEmpty())
        searchKeywords(keywords).map { it.entryId }.toList()
    else emptyList()
}
