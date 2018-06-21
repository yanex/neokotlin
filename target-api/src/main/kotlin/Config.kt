package org.jetbrains.neokotlin

import java.io.File
import java.io.InputStream
import java.util.*

class Config(private val properties: Properties) {
    companion object {
        private const val CONFIGURATION_FILE_NAME = "neokotlin.properties"
        private const val ENVIRON_VARIABLE_PREFIX = "neokotlin."

        fun read(): Config {
            return Config(readConfiguration())
        }

        fun getConfigurationFile(): File {
            val homeDirectory = File(System.getProperty("user.home"))
            if (!homeDirectory.exists()) {
                error("Home directory does not exist: $homeDirectory")
            }

            return File(homeDirectory, Config.CONFIGURATION_FILE_NAME)
        }

        private fun readConfiguration(): Properties {
            val neoKotlinConfigurationFile = getConfigurationFile()
            if (!neoKotlinConfigurationFile.isFile) {
                return readDefaultConfiguration()
            }

            return readConfiguration(neoKotlinConfigurationFile.inputStream())
        }

        fun readDefaultConfiguration(): Properties {
            val resource = Config::class.java.classLoader.getResource("neokotlin.properties")
            return readConfiguration(resource.openStream())
        }

        private fun readConfiguration(ist: InputStream): Properties {
            return Properties().apply {
                ist.use { load(it) }
                // Environment options should override configuration so they go last
                readPropertiesFromEnvironment(this)
            }
        }

        private fun readPropertiesFromEnvironment(properties: Properties) {
            System.getenv()
                .filterKeys { it.startsWith(ENVIRON_VARIABLE_PREFIX) }
                .forEach { properties[it.key.drop(ENVIRON_VARIABLE_PREFIX.length)] = it.value }
        }
    }

    operator fun get(key: String): String {
        val value = properties[key]?.toString()?.takeIf { it.isNotEmpty() }
        return value ?: die(
                "$key is not defined.\n" +
                "Specify it in the configuration file (${getConfigurationFile().absolutePath}).\n" +
                "Use 'neokotlin clone config' to clone the default configuration."
        )
    }
}
