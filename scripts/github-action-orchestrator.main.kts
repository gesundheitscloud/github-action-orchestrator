#!/usr/bin/env kotlin

@file:Repository("https://jcenter.bintray.com/")
@file:Repository("https://dl.bintray.com/jakubriegel/kotlin-shell")
@file:DependsOn("org.jetbrains.kotlin:kotlin-script-runtime:1.3.72")
@file:DependsOn("org.jetbrains.kotlin:kotlin-main-kts:1.3.72")
@file:DependsOn("eu.jrie.jetbrains:kotlin-shell-core:0.2.1")
@file:DependsOn("org.slf4j:slf4j-simple:1.7.28")
@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
@file:DependsOn("io.ktor:ktor-client-core:1.3.2")
@file:DependsOn("io.ktor:ktor-client-cio:1.3.2")
@file:DependsOn("io.ktor:ktor-client-json:1.3.2")
@file:DependsOn("io.ktor:ktor-client-gson:1.3.2")
@file:CompilerOptions("-Xopt-in=kotlin.RequiresOptIn")
@file:CompilerOptions("-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")

import eu.jrie.jetbrains.kotlinshell.shell.*
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.post
import kotlin.system.exitProcess


// ### Data
val organization = "gesundheitscloud"

val repositories = listOf<GitHubRepository>(
        // Apps
        GitHubRepository(organization, "covhub-mobile"),
        GitHubRepository(organization, "mobile-client-android"),
        GitHubRepository(organization, "mobile-client-ios"),

        // SDK Android
        GitHubRepository(organization, "hc-sdk-android"),
        GitHubRepository(organization, "hc-sdk-android-integration"),

        // SDK - iOS
        GitHubRepository(organization, "hc-sdk-ios"),
        GitHubRepository(organization, "hc-sdk-ios-integration"),
        GitHubRepository(organization, "hc-fhir-profiles-ios"),
        GitHubRepository(organization, "hc-fhir-ios"),
        GitHubRepository(organization, "hc-sdk-util-ios")
)

val reposToRemove = listOf<GitHubRepository>(
        // empty
)


// constants
val runnerVersion = "2.168.0"
val runnerFolder = "action-runners"
val runnerUrl = "https://github.com/actions/runner/releases/download/v${runnerVersion}/actions-runner-osx-x64-${runnerVersion}.tar.gz"
val runnerFile = "actions-runner-osx-x64-${runnerVersion}.tar.gz"


shell {
    val accessToken = env("GITHUB_ACTIONS_RUNNER_REGISTRATION_TOKEN")
    if (accessToken.isEmpty()) {
        println("Please provide a GitHub personal access token with repo permission")
        println("and set it as environment var: GITHUB_ACTIONS_RUNNER_REGISTRATION_TOKEN")
        exitProcess(1)
    }

    val client = initHttpClient()

    mkdirp(runnerFolder)
    cd(runnerFolder)

    // Download runner
    if (fileExists(runnerFile).not()){
        curl(runnerUrl)
    }

    for (repo in repositories) {
        println()
        println("------------>")
        println("Processing repository to configure:  ${repo.name}")
        println("------------")

        if (dirExists(repo.name).not()) {
            installRunner(client, repo, accessToken)
        } else {
            println("Runner already installed!")
        }

        println("<-----------")
    }

    for (repo in reposToRemove) {
        println()
        println("------------>")
        println("Processing repository to delete:  ${repo.name}")
        println("------------")

        if (dirExists(repo.name)) {
            uninstallRunner(client, repo, accessToken)
        } else {
            println("Runner already uninstalled!")
        }

        println("<-----------")
    }

    cd(up)
    client.close()

    println("")
    println("Finished successfully")
}

// #### Helper
fun initHttpClient(): HttpClient {
    return HttpClient() {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }
}

// #### Scripts
suspend fun Shell.installRunner(client: HttpClient, repo: GitHubRepository, accessToken: String) {
    println("Installing runner")
    mkdirp(repo.name)
    println("Folder created")

    // copy runner
    copy(runnerFile, repo.name)
    cd(repo.name)
    untar(runnerFile)
    delete(runnerFile)

    // request token
    val repoToken = runnerToken(client, repo, accessToken)
    println("CurrentToken: $repoToken")

    // Configure
    runnerConfig(repo, repoToken)

    println()
    println("Install service and start")
    installService()
    startService()

    cd(up)
    println()
    println("Successfully installed runner!")
}

suspend fun Shell.uninstallRunner(client: HttpClient, repo: GitHubRepository, accessToken: String) {
    println("Uninstalling runner")
    cd(repo.name)

    val repoToken = runnerToken(client, repo, accessToken)

    uninstallService()
    runnerConfigRemove(repoToken)

    cd(up)

    deleteDir(repo.name)
}


suspend fun Shell.runnerToken(client: HttpClient, repo: GitHubRepository, accessToken: String): String {
    val url = "https://api.github.com/repos/${repo.organization}/${repo.name}/actions/runners/registration-token"
    val response: TokenResponse = client.post(url) {
        header("Authorization", "token ${accessToken}")
    }
    return response.token
}

suspend fun Shell.runnerConfig(repo: GitHubRepository, repoToken: String) {
    "./config.sh --url https://github.com/${repo.organization}/${repo.name} --token ${repoToken} --work _work"()
}

suspend fun Shell.runnerConfigRemove(repoToken: String) {
    "./config.sh remove --token ${repoToken}"()
}

suspend fun Shell.installService() {
    "./svc.sh install"()
}

suspend fun Shell.startService() {
    "./svc.sh start"()
}

suspend fun Shell.uninstallService() {
    "./svc.sh uninstall"()
}

// ### Shell
suspend fun Shell.mkdirp(path: String) {
    "mkdir -p $path"()
}

suspend fun Shell.curl(url: String) {
    "curl -O -L $url"()
}

suspend fun Shell.untar(file: String) {
    "tar xzf ${file}"()
}

suspend fun Shell.copy(source: String, destination: String) {
    "cp $source $destination"()
}

suspend fun Shell.delete(file: String) {
    "rm $file"()
}

suspend fun Shell.deleteDir(name: String) {
    "rm -rf $name"()
}

suspend fun Shell.fileCreate(fileName: String) {
    "touch $fileName"()
}

suspend fun Shell.fileExists(fileName: String): Boolean {
    val result = StringBuilder().let {
        pipeline { "ls".process() pipe "grep $fileName".process() pipe it }
        it.toString()
    }
    return result.contains(fileName)
}

suspend fun Shell.dirExists(dirName: String): Boolean {
    val result = StringBuilder().let {
        pipeline { "ls".process() pipe "grep $dirName".process() pipe it }
        it.toString()
    }
    return result.contains(dirName)
}


// #### Data
data class TokenResponse(
        val token: String
)

data class GitHubRepository(
        val organization: String,
        val name: String
)
