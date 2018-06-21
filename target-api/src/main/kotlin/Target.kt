package org.jetbrains.neokotlin

interface Target {
    val command: String
    val description: String

    fun run(args: List<String>, options: Options, config: Config)
    fun usage()

    companion object {
        private const val APP = "neokotlin"

        fun Target.printUsage(parameters: String, vararg additionalText: String): Nothing {
            println(description)
            println("Usage:")

            val parametersWithSpace = if (parameters.isNotEmpty()) " $parameters" else ""
            println("> $APP $command$parametersWithSpace")

            if (additionalText.isNotEmpty()) {
                println()
                additionalText.forEach(::println)
            }

            die()
        }
    }
}