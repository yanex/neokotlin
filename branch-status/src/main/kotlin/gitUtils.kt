package org.jetbrains.neokotlin

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

private const val GITDIR_PREFIX = "gitdir: "

fun openGitRepository(file: File): Repository {
    val gitFile = File(file, ".git")

    return when {
        gitFile.isFile -> (gitFile.readText().lines()
                .firstOrNull { it.startsWith(GITDIR_PREFIX) } ?: error("Invalid '.git' file"))
                .drop(GITDIR_PREFIX.length)
                .let { openGitRepository(File(it)) }
        gitFile.isDirectory -> FileRepositoryBuilder().apply { gitDir = gitFile }.build()
        else -> error("Can't find '.git' file or directory")
    }
}