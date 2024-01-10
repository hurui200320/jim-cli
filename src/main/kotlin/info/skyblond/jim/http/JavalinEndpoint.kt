package info.skyblond.jim.http

import info.skyblond.jim.http.handler.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.InternalServerErrorResponse
import io.javalin.json.jsonMapper
import java.lang.reflect.Type
import javax.crypto.SecretKey

private val logger = KotlinLogging.logger("Endpoint")
private fun Context.reqFromJson(string: String): JimRequest =
    jsonMapper().fromJsonString(string, JimRequest::class.java as Type)

private fun Context.respToJson(resp: JimResponse): String =
    jsonMapper().toJsonString(resp, JimResponse::class.java as Type)

private fun Context.decryptReq(key: SecretKey, debug: Boolean): JimRequest? = runCatching {
    if (debug) reqFromJson(body())
    else reqFromJson(key.decrypt(bodyAsBytes()).decodeToString())
}.onFailure { logger.error(it) { "Failed to decrypt request" } }.getOrNull()

private fun Context.encryptResp(key: SecretKey, debug: Boolean, resp: JimResponse): ByteArray? = runCatching {
    if (debug) contentType(ContentType.APPLICATION_JSON)
    else contentType(ContentType.APPLICATION_OCTET_STREAM)
    if (debug) respToJson(resp).toByteArray()
    else key.encrypt(respToJson(resp).toByteArray())
}.onFailure { logger.error(it) { "Failed to encrypt response" } }.getOrNull()


fun Javalin.registerEndPoint(key: SecretKey, debug: Boolean): Javalin = this.apply {
    if (debug) logger.warn { "Debug mode on, endpoint will not be encrypted" }
    // considering we're using HTTP in the private network,
    // we need to find a way for secure communication.
    // Use SHA-256 for key generation
    // and AES/GCM/NoPadding to encrypt the body
    post("/") { ctx ->
        val req = ctx.decryptReq(key, debug)
            ?: throw BadRequestResponse("Can't decode")

        val r = runCatching {
            handleReq(req.command.lowercase(), req.params)
        }.onFailure { logger.error(it) { "Failed to process request $req" } }

        val resp = if (r.isSuccess) {
            respOk(req.requestId, r.getOrNull())
        } else {
            respErr(req.requestId, r.exceptionOrNull()!!.message ?: "Unknown error")
        }

        ctx.result(
            ctx.encryptResp(key, debug, resp)
                ?: throw InternalServerErrorResponse("Cannot generate response")
        )
    }
}

// command is always lowercase, use `_` instead of space
// same as cli command
private fun handleReq(command: String, params: List<*>): Any = when (command) {
    "browse" -> handleBrowse(params)
    "create_entry" -> handleCreateEntry(params)
    "create_meta" -> handleCreateMeta(params)
    "delete_entry" -> handleDeleteEntry(params)
    "update_entry" -> handleUpdateEntry(params)
    "update_meta" -> handleUpdateMeta(params)
    "delete_meta" ->  handleDeleteMeta(params)
    "search" -> handleSearch(params)
    "view" -> handleView(params)
    else -> throw BadRequestResponse("Unknown command")
}
