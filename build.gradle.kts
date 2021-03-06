import org.cadixdev.gradle.licenser.LicenseExtension

plugins {
    jacoco
    id("org.cadixdev.licenser") version "0.5.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.cadixdev.licenser")

    configure<LicenseExtension> {
        exclude {
            it.file.startsWith(project.buildDir)
        }
        header = rootProject.file("HEADER.txt")
        (this as ExtensionAware).extra.apply {
            for (key in listOf("organization", "url")) {
                set(key, rootProject.property(key))
            }
        }
    }

    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(16))
        }
    }
}

val totalReport = tasks.register<JacocoReport>("jacocoTotalReport") {
    reports {
        xml.isEnabled = true
        xml.destination = rootProject.buildDir.resolve("reports/jacoco/report.xml")
        html.isEnabled = true
    }
    subprojects.forEach { proj ->
        proj.plugins.withId("java") {
            proj.plugins.withId("jacoco") {
                executionData(
                    fileTree(proj.buildDir.absolutePath).include("**/jacoco/*.exec")
                )
                sourceSets(proj.the<JavaPluginConvention>().sourceSets["main"])
                dependsOn(proj.tasks.named("test"))
            }
        }
    }
}
afterEvaluate {
    totalReport.configure {
        classDirectories.setFrom(classDirectories.files.map {
            fileTree(it).apply {
                exclude("**/protos/*.class")
            }
        })
    }
}
