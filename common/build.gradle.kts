plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Add common scoped dependencies here
    compileOnly(libs.ignition.common)
}
