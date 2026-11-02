plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.raufpnl.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.raufpnl.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val keyPath = System.getenv("RAUF_KEYSTORE_PATH")
            val keyStorePassword = System.getenv("RAUF_KEYSTORE_PASSWORD")
            val keyAliasName = System.getenv("RAUF_KEY_ALIAS")
            val keyPasswordValue = System.getenv("RAUF_KEY_PASSWORD")
            if (!keyPath.isNullOrBlank() && !keyStorePassword.isNullOrBlank() &&
                !keyAliasName.isNullOrBlank() && !keyPasswordValue.isNullOrBlank()) {
                storeFile = file(keyPath)
                storePassword = keyStorePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val signingValuesPresent = listOf(
                "RAUF_KEYSTORE_PATH", "RAUF_KEYSTORE_PASSWORD", "RAUF_KEY_ALIAS", "RAUF_KEY_PASSWORD"
            ).all { !System.getenv(it).isNullOrBlank() }
            if (signingValuesPresent) signingConfig = signingConfigs.getByName("release")
        }
    }
}
