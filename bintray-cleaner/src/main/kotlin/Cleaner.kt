package org.jetbrains.neokotlin

import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.jetbrains.neokotlin.Target.Companion.printUsage
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object Cleaner : Target {
    override val command = "bintray clean"
    override val description = "Cleans obsolete BinTray artifacts"

    override fun run(args: List<String>, options: Options, config: Config) {
        val repoOwner = config["bintray.owner"]
        val credentials = Credentials.basic(config["bintray.user"], config["bintray.api.key"])

        val (repoName, packageName) = args.takeIf { it.size == 2 } ?: usage()

        val httpClient = OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://bintray.com/api/v1/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        val bintrayService = retrofit.create(BintrayService::class.java)

        val versions = bintrayService.getPackage(repoOwner, repoName, packageName)().versions
        if (options.isVerbose) {
            println("Found ${versions.size} version(s)")
        }

        val oneMonthFromNow = Date(System.currentTimeMillis() - 1000L * 3600 * 24 * 30) // 1 month

        val preserveCount = 3
        val devsToPreserve = versions.asSequence()
                .filter { ArtifactType.parse(it) == ArtifactType.DEV }
                .take(preserveCount).toSet()

        for (versionId in versions.reversed()) {
            val artifactType = ArtifactType.parse(versionId)
            if (artifactType != ArtifactType.DEV && artifactType != ArtifactType.EAP) {
                if (options.isVerbose) {
                    println("$versionId: skipping $artifactType publication")
                }
                continue
            }

            if (versionId in devsToPreserve) {
                if (options.isVerbose) {
                    println("$versionId: skipping, preserving last $preserveCount build(s)")
                }
                continue
            }

            val version = bintrayService.getVersion(credentials, repoOwner, repoName, packageName, versionId)()
            val publishingDate = DateFormat.getDateInstance().format(version.created)

            if (version.created.after(oneMonthFromNow)) {
                if (options.isVerbose) {
                    println("$versionId: skipping, relatively new ($publishingDate)")
                }
                continue
            }

            fun deleteCurrent() {
                if (options.isDryRun) {
                    println("ok (dry run).")
                    return
                }

                lateinit var result: Response<Unit>
                val took = measureTimeMillis {
                    result = bintrayService.deleteVersion(
                            credentials, repoOwner, repoName, packageName, versionId).execute()
                } / 1000
                println(if (result.isSuccessful) "ok (took $took s)." else result.raw().message())
            }

            if (options.isNonInteractive) {
                print("$versionId: Deleting obsolete version ($publishingDate)... ")
                deleteCurrent()
            } else {
                print("$versionId: obsolete version ($publishingDate). Delete (y/n)? ")
                if ((readLine() ?: return).toLowerCase() == "y") {
                    print("deleting... ")
                    deleteCurrent()
                }
            }
        }
    }

    private enum class ArtifactType {
        EAP, DEV, OTHER;

        companion object {
            private val REGEX = "\\d+\\.\\d+(?:\\.\\d+)?(?:-[A-Za-z0-9]+)?(?:-([a-z]+)-\\d+)?".toRegex()

            fun parse(versionId: String): ArtifactType {
                val qualifier = REGEX.matchEntire(versionId)?.groupValues?.drop(1)?.firstOrNull() ?: return OTHER
                return when (qualifier) {
                    "eap", "rc" -> EAP
                    "dev" -> DEV
                    else -> OTHER
                }
            }
        }
    }

    override fun usage(): Nothing = printUsage("<repo> <package>",
            "You probably want to clean 'kotlin-dev' / 'kotlin'.",
            "If yes, run '$command kotlin-dev kotlin'.")
}

private operator fun <T> Call<T>.invoke(): T {
    val result = execute()
    if (!result.isSuccessful) {
        die(result.raw().message())
    }
    return result.body()!!
}