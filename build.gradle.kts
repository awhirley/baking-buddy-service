
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(ktorLibs.plugins.ktor)
  kotlin("plugin.serialization") version "2.4.0"
  id("dev.detekt") version ("2.0.0-alpha.6")
  id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "com.bakingbuddy"
version = "1.0.0-SNAPSHOT"

application {
  mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(ktorLibs.server.config.yaml)
  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.cors)
  implementation(ktorLibs.server.netty)

  implementation("io.ktor:ktor-server-content-negotiation")
  implementation("io.ktor:ktor-serialization-kotlinx-json")
  implementation("io.ktor:ktor-server-status-pages")
  implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

  implementation("io.ktor:ktor-server-openapi:3.5.2")
  implementation("io.ktor:ktor-server-routing-openapi:3.5.2")

  implementation("org.jetbrains.exposed:exposed-core:1.3.1")
  implementation("org.jetbrains.exposed:exposed-jdbc:1.3.1")
  implementation("org.jetbrains.exposed:exposed-java-time:1.3.1")
  implementation("org.postgresql:postgresql:42.7.7")

  implementation(libs.logback.classic)

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
  testImplementation("io.kotest:kotest-assertions-core:5.9.1")
  testImplementation("io.mockk:mockk:1.13.11")
  testImplementation("io.ktor:ktor-server-test-host")
  testImplementation("io.ktor:ktor-client-content-negotiation")
  testImplementation("io.ktor:ktor-serialization-kotlinx-json")
}

detekt {
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom(files("${rootProject.projectDir}/detekt.yml"))
}

tasks.named("check") {
  dependsOn("detekt")
}

tasks.test {
  useJUnitPlatform()
}
