import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appDisplayName = "Local File Sender"
val appPackageName = "LocalFileSender"
val appVersion = providers.gradleProperty("appVersion").orElse("1.0.0").get()

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
            packageName = appPackageName
            packageVersion = appVersion
            description = appDisplayName

            windows {
                iconFile.set(project.file("src/main/resources/icons/app-icon.ico"))
                menuGroup = appDisplayName
                menu = true
                shortcut = true
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icons/app-icon.icns"))
                dockName = appDisplayName
                setDockNameSameAsPackageName = false
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/app-icon.png"))
                menuGroup = appDisplayName
                shortcut = true
            }
        }
    }
}
