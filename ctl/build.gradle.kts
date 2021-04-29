plugins {
    java
    application
    jacoco
}

java {
    withSourcesJar()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)

    implementation(libs.guava)

    implementation(project(":common"))

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
