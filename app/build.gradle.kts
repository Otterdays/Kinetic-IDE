import java.util.Properties

plugins {
    id("com.android.application") version "9.2.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
    id("com.google.devtools.ksp") version "2.3.6"
    id("com.google.dagger.hilt.android") version "2.59.2"
}

val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { localProps.load(it) }
}

val anthropicKey: String =
    (project.findProperty("anthropicApiKey") as String?)
        ?: localProps.getProperty("anthropicApiKey")
        ?: ""

val geminiKey: String =
    (project.findProperty("geminiApiKey") as String?)
        ?: localProps.getProperty("geminiApiKey")
        ?: ""

val openAiKey: String =
    (project.findProperty("openAiApiKey") as String?)
        ?: localProps.getProperty("openAiApiKey")
        ?: ""

val grokKey: String =
    (project.findProperty("grokApiKey") as String?)
        ?: localProps.getProperty("grokApiKey")
        ?: ""

val openRouterKey: String =
    (project.findProperty("openrouterApiKey") as String?)
        ?: localProps.getProperty("openrouterApiKey")
        ?: ""

val githubOAuthClientId: String =
    (project.findProperty("githubOAuthClientId") as String?)
        ?: localProps.getProperty("githubOAuthClientId")
        ?: ""

android {
    namespace = "com.tabletaide.ide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tabletaide.ide"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"${anthropicKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"${openAiKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "GROK_API_KEY", "\"${grokKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${openRouterKey.replace("\"", "\\\"")}\"")
        buildConfigField(
            "String",
            "GITHUB_OAUTH_CLIENT_ID",
            "\"${githubOAuthClientId.replace("\"", "\\\"")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.browser:browser:1.8.0")

    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.6.0.202603022253-r")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
