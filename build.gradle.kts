tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

allprojects {
    afterEvaluate {
        if (tasks.findByName("prepareKotlinBuildScriptModel") == null) {
            tasks.register("prepareKotlinBuildScriptModel") {
                group = "help"
                description = "Compatibility no-op for IDE Gradle sync with AGP built-in Kotlin."
            }
        }
    }
}
