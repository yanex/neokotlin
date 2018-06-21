package org.jetbrains.neokotlin

private val targets: List<Target> = listOf(Cleaner, BranchStatus, CloneConfig)

object NeoKotlin {
    @JvmStatic
    fun main(args: Array<String>) {
        val (freeArgs, optionArgs) = args.partition { !it.startsWith("-") }
        val options = Options(optionArgs.map { it.drop(1) }.toSet())

        val targetToLaunch = targets.singleOrNull { target ->
            val commandChunks = target.command.split(' ').takeIf { freeArgs.size >= it.size }
                    ?: return@singleOrNull false
            commandChunks.withIndex().all { (i, c) -> freeArgs[i] == c }
        } ?: unknownTarget(freeArgs)

        val freeArgsForTarget = freeArgs.drop(targetToLaunch.command.split(' ').size)

        try {
            targetToLaunch.run(freeArgsForTarget, options, Config.read())
        } catch (thr: Throwable) {
            System.err.println("An error occurred: ${thr.message}")
            if (options.isVerbose) {
                thr.printStackTrace()
            }
            System.exit(1)
        }
    }

    private fun unknownTarget(freeArgs: List<String>): Nothing {
        System.err.apply {
            println("Unknown target: ${freeArgs.joinToString(" ")}")
            println()
            println("Available commands:")

            val targetDescriptions = targets.map { Pair(it.command, it.description) }

            val commandMaxLength = targetDescriptions.maxBy { it.first.length }!!.first.length

            for ((command, description) in targetDescriptions) {
                println(command + " ".repeat(commandMaxLength - command.length) + "   " + description)
            }
        }

        die()
    }
}