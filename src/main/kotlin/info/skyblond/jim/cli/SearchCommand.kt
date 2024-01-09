package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.colormath.model.Oklch
import com.github.ajalt.mordant.rendering.TextStyle
import info.skyblond.jim.core.prettyString
import info.skyblond.jim.core.searchKeywords
import kotlin.math.absoluteValue

object SearchCommand : CliktCommand(
    name = "search",
    help = "Search entries with a set of keywords"
) {
    private val keywords by argument("keyword")
        .multiple()
        .check("Keyword must be at least 2 characters") { l ->
            l.all { it.length >= 2 }
        }

    private val disableColor by option("-n", "--no-color")
        .flag()
        .help("Disable color highlighting to save time on processing result")

    private val colorBrightness by option("-b", "--brightness")
        .float()
        .default(0.8f)
        .help("Choosing the color brightness (OkLCH) for visualization, range: `[0,1]`, default 0.8")
        .check("Brightness must in [0,1]") { it in 0f..1f }

    private val colorChroma by option("-c", "--chroma")
        .float()
        .default(0.3f)
        .help("Choosing the color chroma (OkLCH) for visualization, range: `[0,1]`, default 0.3")
        .check("Chroma must in [0,1]") { it in 0f..1f }

    private val haveFun by option("--fun")
        .float()
        .default(Float.NaN)
        .help("Have some fun with OkLCH, given the chroma")
        .check("Chroma must in [0,1]") { it.isNaN() || it in 0f..1f }

    // here we use oklab for all keywords to have equal brightness
    private val colors by lazy {
        if (disableColor) emptyList()
        else buildList {
            for (i in 0..360 step 20) {
                add(TextStyle(Oklch(colorBrightness, colorChroma, i.toFloat())))
            }
        }
    }

    override fun run() {
        if (haveFun.isNaN()) {
            searchKeywords(keywords).forEach { e ->
                echo(
                    e.prettyString(
                        keywords = if (disableColor) emptyList() else keywords
                    ) {
                        val index = (it.hashCode() % colors.size).absoluteValue
                        colors[index](it)
                    }
                )
            }
        } else {
            val height = terminal.info.height - 2
            for (y in 0..height) {
                // leave 2 extra chars for l
                val width = terminal.info.width - 3
                val l = y / height.toFloat()
                echo("00${(l * 100).toInt()}".takeLast(2), trailingNewline = false)
                for (x in 0..width) {
                    val s = TextStyle(
                        Oklch(
                            l, haveFun,
                            x * 360.0 / width
                        )
                    )
                    echo(s("\u2588"), trailingNewline = false)
                }
                echo()
            }
        }
    }

}
