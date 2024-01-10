package info.skyblond.jim.http

import kotlin.reflect.KClass
import kotlin.reflect.full.cast

private fun <T : Any> List<*>.castParam(
    index: Int, type: KClass<T>, nullable: Boolean
): T? {
    val raw = this.getOrNull(index)
    require(nullable || raw != null) { "NonNull parameter at index $index is null" }
    if (nullable && raw == null) return null
    require(type.isInstance(raw)) { "Parameter at index $index is not of type ${type.qualifiedName}" }
    return type.cast(raw)
}
fun <T : Any> List<*>.castParam(
    index: Int, type: KClass<T>
): T = this.castParam(index, type, false)!!

fun <T : Any> List<*>.castParamNullable(
    index: Int, type: KClass<T>
): T? = this.castParam(index, type, true)
