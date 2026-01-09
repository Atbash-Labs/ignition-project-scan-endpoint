plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {

    api(project(":common"))

    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
}
