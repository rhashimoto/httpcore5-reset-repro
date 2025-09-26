plugins {
  kotlin("jvm") version "2.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.3.6")
  implementation("org.apache.httpcomponents.core5:httpcore5-testing:5.3.6")
  implementation("org.conscrypt:conscrypt-openjdk-uber:2.5.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.slf4j:slf4j-api:2.0.17")
  implementation("org.slf4j:slf4j-simple:2.0.17")
  testImplementation(kotlin("test"))
}

tasks.jar.configure {
  manifest {
    attributes(mapOf("Main-Class" to "MainKt"))
  }
  configurations["compileClasspath"].forEach { file ->
    from(zipTree(file.absoluteFile))
  }
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
  exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(24)
}