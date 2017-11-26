/*
 * MIT License
 *
 * Copyright (c) 2017 Taizo Kusuda 
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ryuta46.nemkotlin.net

import com.ryuta46.nemkotlin.exceptions.ParseException

class StompFrame(val command: Command, val headers: Map<String,String> = emptyMap(), val body: String = "") {
    enum class Command {
        // Client -> Server frames
        Connect,
        Send,
        Subscribe,
        Unsubscribe,
        Begin,
        Commit,
        Abort,
        Ack,
        Nack,
        Disconnect,
        // Server -> Client frames
        Connected,
        Message,
        Receipt,
        Error;
        companion object {
            @JvmStatic fun fromString(commandString: String): Command? =
                values().find { it.name.toUpperCase() == commandString.toUpperCase() }
        }

    }

    companion object {
        @JvmStatic fun parse(frameString: String): StompFrame {
            val sections = frameString.split("\n\n")
            if (sections.size != 2) {
                throw ParseException("Failed to split to headers and body.")
            }
            val metaLines = sections[0].split("\n")
            val command = Command.fromString(metaLines[0]) ?: throw ParseException("Unknown command: ${metaLines[0]}")

            val headers = mutableMapOf<String, String>()

            headers.putAll(metaLines.drop(1).map {
                val kv = it.split(":", limit = 2)
                if (kv.size != 2) throw ParseException("Failed to parse header: $it")
                Pair(kv[0], kv[1])
            })
            // drop \0
            val body = sections[1].dropLast(1)

            return StompFrame(command, headers, body)
        }
    }

    override fun toString(): String {
        return command.name.toUpperCase() + "\n" +
                headers.toList().joinToString(separator = "") { "${it.first}:${it.second}\n" } + "\n" +
                body + "\u0000"
    }

    val description: String
        get() =
"""
command:
  ${command.name.toUpperCase()}
headers:${headers.toList().joinToString(separator = "") { "\n  ${it.first}:${it.second}" }}
body:
  $body
"""
    val lineDescription: String
        get() = "command=${command.name.toUpperCase()}, headers={${headers.toList().joinToString(separator = ",") { "${it.first}:${it.second}" }}} body=$body"
}

