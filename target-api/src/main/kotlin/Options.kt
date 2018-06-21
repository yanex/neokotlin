package org.jetbrains.neokotlin

class Options(options: Set<String>) : Set<String> by options {
    val isTiny = "t" in this || "-tiny" in this
    val isVerbose = "v" in this || "-verbose" in this
    val isNonInteractive = "ni" in this || "-non-interactive" in this
    val isDryRun = "n" in this || "-dry-run" in this
}