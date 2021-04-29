plugins {
    java
    application
    jacoco
}

java {
    withSourcesJar()
}

application.mainClass.set("net.octyl.rss.dokusha.DokushaDaemon")

dependencies {
    compileOnly(libs.jetbrains.annotations)

    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4jImpl)

    implementation(libs.guava)

    implementation(project(":common"))

    implementation(libs.rome.core)

    implementation(libs.mapdb)

    implementation(libs.simpleJavaMail.core)
    implementation(libs.simpleJavaMail.batch)
    implementation(libs.simpleJavaMail.smime)
    implementation(libs.simpleJavaMail.dkim)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.truth) {
        exclude(group = "junit")
    }
}

tasks.test {
    useJUnitPlatform()
}
