plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    java
    `maven-publish`
}

repositories {
    maven("https://m2.dv8tion.net/releases")
    maven {
        url = uri("https://jcenter.bintray.com")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.google.api-client:google-api-client:1.23.0")
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("net.dv8tion", "JDA", "4.3.0_339") {
        exclude(module = "opus-java")
    }
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.apache.logging.log4j:log4j-core:2.17.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok", "lombok", "1.18.22")
}

group = "de.eldoria"
version = "1.0"
description = "KingOfTheHill"
java.sourceCompatibility = JavaVersion.VERSION_11

tasks {
    shadowJar {
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "de.eldoria.kingofthehill.KingOfTheHill"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
    }

    javadoc {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
