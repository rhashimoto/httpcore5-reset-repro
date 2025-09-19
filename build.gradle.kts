plugins {
  kotlin("jvm") version "2.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.3.5")
  implementation("org.conscrypt:conscrypt-openjdk-uber:2.5.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}