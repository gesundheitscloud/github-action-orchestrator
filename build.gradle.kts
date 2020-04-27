plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"

    // https://github.com/ben-manes/gradle-versions-plugin
    id("com.github.ben-manes.versions") version "0.28.0"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/jakubriegel/kotlin-shell")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-script-util:1.3.72")
    implementation("org.apache.ivy:ivy:2.5.0")


    // Script dependencies
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-main-kts:1.3.72")
    implementation("eu.jrie.jetbrains:kotlin-shell-core:0.2.1")
    implementation("org.slf4j:slf4j-simple:1.7.28")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("io.ktor:ktor-client-core:1.3.2")
    implementation("io.ktor:ktor-client-cio:1.3.2")
    implementation("io.ktor:ktor-client-json:1.3.2")
    implementation("io.ktor:ktor-client-gson:1.3.2")
}
