package org.jetbrains.neokotlin

import org.jetbrains.neokotlin.Target.Companion.printUsage

object CloneConfig : Target {
    override val command = "clone config"
    override val description = "Clones the default configuration file"

    override fun run(args: List<String>, options: Options, config: Config) {
        val configurationFile = Config.getConfigurationFile()
        if (configurationFile.exists()) {
            die("'${configurationFile.absolutePath}' already exists")
        }

        configurationFile.bufferedWriter().use { writer ->
            Config.readDefaultConfiguration().store(writer, null)
        }

        println("Copied to '${configurationFile.absolutePath}'.")
    }

    override fun usage(): Nothing = printUsage("")
}