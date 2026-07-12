import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LeopardCharts"
            isStatic = true
        }
    }
    
    jvm("desktop") {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.github.leopard.charts"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}


publishing {
    publications.withType<MavenPublication> {
        val pubName = name
        val javadocTask = tasks.register("${pubName}JavadocJar", Jar::class) {
            archiveClassifier.set("javadoc")
            archiveAppendix.set(pubName)
            from(tasks.named("dokkaHtml"))
        }
        artifact(javadocTask.get())

        pom {
            name.set("Leopard")
            description.set("A premium, highly interactive, and beautiful data visualization library built entirely with Compose Multiplatform.")
            url.set("https://github.com/YutaroSuzuki-JP/leopard")

            licenses {
                license {
                    name.set("The MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("yutarosuzuki-jp")
                    name.set("Yutaro Suzuki")
                    email.set("i.buzzbuzzinc@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/YutaroSuzuki-JP/leopard.git")
                developerConnection.set("scm:git:ssh://github.com/YutaroSuzuki-JP/leopard.git")
                url.set("https://github.com/YutaroSuzuki-JP/leopard")
            }
        }
    }

    repositories {
        maven {
            name = "layout"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
