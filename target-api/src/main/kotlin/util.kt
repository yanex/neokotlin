package org.jetbrains.neokotlin

fun die(errorMessage: String = "", exitCode: Int = 1): Nothing {
    errorMessage.takeIf { it.isNotEmpty() }?.let(::println)
    System.exit(exitCode)
    error("System.exit() doesn't work on your JVM")
}