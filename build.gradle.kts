plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.version.catalog.update)
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
