package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal

fun CliktCommand.echoErr(message: String) {
    echo(terminal.theme.danger(message), err = true)
}

fun CliktCommand.displayWhenError(block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        echoErr("Error: ${t.message}")
    }
}
