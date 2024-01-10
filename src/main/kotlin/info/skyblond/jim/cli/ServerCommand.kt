package info.skyblond.jim.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import info.skyblond.jim.http.registerEndPoint
import info.skyblond.jim.http.sha256KeyGen
import io.javalin.Javalin
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector

object ServerCommand : CliktCommand(
    name = "server",
    help = "Setup a HTTP server for other application to use"
) {
    private val interfaces by option("-l", "--listen")
        // 0.0.0.0 includes ipv6
        .multiple(default = listOf("0.0.0.0"))
        .help("Listen on which interface (ip addresses)")

    private val listenPort by option("-p", "--port").int()
        .help("Listen on which port")
        .default(8080)
        .help("Which port to listen")
        .check("Invalid port") { it in 1..65535 }

    private val debug by option("-d", "--debug").flag()
        .help("Disable endpoint encryption, for debug purpose only")

    private val password by argument(name = "password")
        .help("Password for encrypt endpoint")

    override fun run() {
        Javalin.create {
            it.jetty.server {
                Server().also { server ->
                    server.connectors = interfaces.map { ip ->
                        ServerConnector(server).apply {
                            host = ip
                            port = listenPort
                        }
                    }.toTypedArray()
                }
            }
        }.registerEndPoint(
            key = sha256KeyGen(password),
            debug = debug
        ).start()
    }
}
