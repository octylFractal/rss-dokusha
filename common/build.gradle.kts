import com.google.protobuf.gradle.ProtobufConvention
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    `java-library`
    jacoco
    id("com.google.protobuf") version "0.8.16"
}

java {
    withSourcesJar()
    withJavadocJar()
}

configurations {
    register("protocLocal") {
        resolutionStrategy.eachDependency {
            if (target.name == "protoc") {
                artifactSelection {
                    selectArtifact(
                        "jar",
                        "exe",
                        project.osdetector.classifier
                    )
                }
            }
        }
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.slf4jImpl)

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.properties)

    api(libs.guava)

    api(libs.fluentResult)

    api(platform(libs.reactor.bom))
    api(libs.reactor.core)

    api(platform(libs.protobuf.bom))
    api(libs.protobuf.core)

    api(libs.directories)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(libs.log4j.core)

    testImplementation(libs.reactor.test)

    testImplementation(libs.truth) {
        exclude(group = "junit")
    }

    "protocLocal"(libs.protobuf.protoc)
}

configure<ProtobufConvention> {
    protobuf {
        protoc {
            path = configurations["protocLocal"].resolve().single().absolutePath
        }
    }
}

val pbuf = convention.getPlugin<ProtobufConvention>()
val protobufGenOut = file("${pbuf.protobuf.generatedFilesBaseDir}/main/java")
sourceSets {
    main {
        java.srcDir(protobufGenOut)
    }
}

plugins.withType<IdeaPlugin>().configureEach {
    model.module.generatedSourceDirs.add(protobufGenOut)
}

tasks.test {
    useJUnitPlatform()
}
