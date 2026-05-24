import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.dimonoso.crosplatformfilesender.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.dimonoso.crosplatformfilesender"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("src/main/resources/icons/app-icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icons/app-icon.icns"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/app-icon.png"))
            }
        }
    }
}
