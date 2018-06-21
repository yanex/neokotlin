package org.jetbrains.neokotlin

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk
import org.jetbrains.neokotlin.Target.Companion.printUsage
import java.io.File
import java.util.*

object BranchStatus : Target {
    private const val ORIGIN_REMOTE_NAME = "origin"
    private const val REMOTE_REF_PREFIX = "refs/remotes/$ORIGIN_REMOTE_NAME/"
    private const val REMOTE_HEAD = "refs/remotes/$ORIGIN_REMOTE_NAME/HEAD"
    private const val REMOTE_MASTER_NAME = "${REMOTE_REF_PREFIX}master"

    private val REMOTE_VERSION_BRANCH_REGEX =
            "${REMOTE_REF_PREFIX}1\\.\\d+(\\.\\d+)?(\\-[A-Za-z0-9]+)?(_/?[A-Za-z0-9\\-]+)?".toRegex()

    private val REMOTE_MVERSION_BRANCH_REGEX =
            "${REMOTE_REF_PREFIX}M\\d+_?(_internal_)?(/idea_continuous)?(/[\\w\\.]+)?".toRegex()

    private val MASTER_PATCHSET_BRANCH_REGEX = "${REMOTE_REF_PREFIX}master_(base_)?\\w+".toRegex()

    override val command = "branch status"
    override val description = "Shows the detailed statistics about Kotlin branches"

    override fun run(args: List<String>, options: Options, config: Config) {
        val repositoryPath = args.singleOrNull() ?: usage()
        val repository = openGitRepository(File(repositoryPath))
        val git = Git(repository)
        calculateBranches(git, options)
    }

    private fun calculateBranches(git: Git, options: Options) {
        val b = splitBranches(git)

        println("Version branches:")
        println("    Version count: " + b.version.size)
        println("    Branch count: " + b.version.values.sumBy { it.size })
        println()

        println("User branches:")

        val totalUserBranchCount = b.user.values.sumBy { it.size }
        val obsoleteUserBranchCount = b.user.values.flatMap { it }.filter { it.isObsolete }.size

        println("    Total branch count: $obsoleteUserBranchCount / $totalUserBranchCount")
        for ((index, userBranches) in b.user.toList().sortedByDescending { it.second.size }.withIndex()) {
            val (user, branches) = userBranches
            val branchesToPrint = if (options.isVerbose) branches else branches.filter { it.isObsolete }

            if (branchesToPrint.isEmpty()) {
                continue
            }

            println("    ${index + 1}: $user - ${branches.count { it.isObsolete }} / ${branches.size}")

            if (!options.isTiny) {
                branchesToPrint.forEach { println("        $it") }
            }
        }

        if (b.wrongEmails.isNotEmpty()) {
            println()
            println("Strange emails found:")
            b.wrongEmails.forEach { println("    $it") }
        }
    }

    private fun splitBranches(git: Git): SplitBranches {
        val revWalk = RevWalk(git.repository)

        val branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
                .filter { it.name.startsWith(REMOTE_REF_PREFIX) && it.name != REMOTE_HEAD }

        lateinit var masterBranch: Ref
        val versionBranches = mutableMapOf<String, MutableList<Branch>>()
        val userBranches = mutableMapOf<Person, MutableList<Branch>>()

        fun parseBranch(ref: Ref): Branch {
            val commit = revWalk.parseCommit(ref.objectId)
            val time = Date(commit.commitTime.toLong() * 1000L)
            return Branch(ref, ref.name.drop(REMOTE_REF_PREFIX.length), time)
        }

        val personMerger = PersonMerger()

        for (branch in branches) {
            val name = branch.name

            if (name == REMOTE_MASTER_NAME) {
                masterBranch = branch
            } else if (REMOTE_VERSION_BRANCH_REGEX.matches(name)
                    || REMOTE_MVERSION_BRANCH_REGEX.matches(name)
                    || MASTER_PATCHSET_BRANCH_REGEX.matches(name)
            ) {
                val version = name.drop(REMOTE_REF_PREFIX.length).substringBefore('_').substringBefore('/')
                versionBranches.getOrPut(version) { mutableListOf() } += parseBranch(branch)
            } else {
                val committer = revWalk.parseCommit(branch.objectId).committerIdent
                val person = personMerger[Person(committer.name, committer.emailAddress)]
                userBranches.getOrPut(person) { mutableListOf() } += parseBranch(branch)
            }
        }

        return SplitBranches(
                masterBranch,
                versionBranches,
                userBranches.mapValues { (_, v) -> v.sortedBy { it.updatedAt } },
                personMerger.getWrongEmails()
        )
    }

    override fun usage(): Nothing = printUsage("[<path>]")
}

private class SplitBranches(
    val master: Ref,
    val version: Map<String, List<Branch>>,
    val user: Map<Person, List<Branch>>,
    val wrongEmails: Set<String>
)