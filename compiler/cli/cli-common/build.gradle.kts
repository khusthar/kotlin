import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:util.runtime"))
    api(project(":compiler:config"))
    api(project(":compiler:config.jvm"))
    api(project(":js:js.config"))
    api(project(":wasm:wasm.config"))
    api(project(":native:kotlin-native-utils"))
    api(project(":compiler:plugin-api"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" {}
}

optInToExperimentalCompilerApi()

tasks.getByName<Jar>("jar") {
    //excludes unused bunch files
    exclude("META-INF/extensions/*.xml.**")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
