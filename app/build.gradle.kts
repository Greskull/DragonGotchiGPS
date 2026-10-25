plugins {
    id("com.android.application")
    // AGP 9.x: Kotlin integrado; NO aplicar "org.jetbrains.kotlin.android".
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dragongotchi.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dragongotchi.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.1.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// AGP 9 / Kotlin integrado: las opciones del compilador ya no van en
// android.kotlinOptions{} sino en kotlin.compilerOptions{}.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// ============================================================
//  BLINDAJE ANTI "Duplicate resources"
// ============================================================
// El error "Duplicate resources" de :app:mergeDebugResources aparece
// cuando dos ficheros definen el mismo recurso: p. ej. un PNG y un XML
// con el mismo nombre en la misma carpeta (stage_01.png + stage_01.xml
// en drawable/) o el mismo name="" repetido en values/*.xml.
// Esta tarea se ejecuta ANTES de compilar (preBuild) y falla con un
// mensaje claro indicando los ficheros en conflicto, en vez del error
// generico de AAPT a mitad de la compilacion.
val checkDuplicateResources = tasks.register("checkDuplicateResources") {
    description = "Detecta recursos duplicados que provocarian 'Duplicate resources' en mergeDebugResources."
    group = "verification"
    doLast {
        val resDir = file("src/main/res")
        val duplicates = mutableListOf<String>()
        // 1) Misma carpeta + mismo nombre (aunque cambie la extension)
        resDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            dir.listFiles()
                ?.groupBy { it.name.substringBefore(".") }
                ?.forEach { (base, files) ->
                    if (files.size > 1) {
                        duplicates += "${dir.name}/$base -> ${files.map { it.name }.sorted()}"
                    }
                }
        }
        // 2) Mismo type+name repetido dentro de values/*.xml
        val valuesDir = File(resDir, "values")
        val namePattern = Regex("""<(string|color|dimen|bool|integer|plurals|style|array|item)\b[^>]*\bname="([^"]+)"""")
        val seen = mutableMapOf<String, String>()
        valuesDir.listFiles { f -> f.extension == "xml" }?.sorted()?.forEach { xml ->
            namePattern.findAll(xml.readText()).forEach { match ->
                val key = "${match.groupValues[1]}/${match.groupValues[2]}"
                val previous = seen.put(key, xml.name)
                if (previous != null) duplicates += "values: $key en ${xml.name} y $previous"
            }
        }
        if (duplicates.isNotEmpty()) {
            throw GradleException(
                "Recursos duplicados detectados (provocarian 'Duplicate resources'):\n" +
                    duplicates.joinToString("\n") +
                    "\nDeja un solo fichero por nombre y carpeta (p. ej. o el PNG o el XML, no ambos)."
            )
        }
    }
}
tasks.named("preBuild") { dependsOn(checkDuplicateResources) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Solo se usan iconos basicos (Home, PlayArrow, List, Settings), incluidos en
    // material-icons-core que ya trae Material3: no cargar material-icons-extended
    // (es enorme y dispararia el tamano de la APK y la memoria al dexar).
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // GPS de alta precision (Fused Location Provider)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Publicidad: banner de AdMob en la pantalla del mapa (Mobile Ads SDK)
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    // Consentimiento GDPR/UMP (Europa): mensaje de privacidad de Google
    // obligatorio antes de servir anuncios personalizados en el EEE.
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    // Mapa GRATUITO con OpenStreetMap (sin API key ni cuenta): osmdroid
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Persistencia: historial de rutas (Room) + ajustes/nivel (DataStore)
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
