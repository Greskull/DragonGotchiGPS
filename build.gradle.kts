// Top-level build file
// AGP 9.3.3: Kotlin viene INTEGRADO en AGP (KGP 2.2.10), por lo que el
// plugin "org.jetbrains.kotlin.android" ya no se declara ni se aplica.
// El plugin de Compose debe coincidir con la version de Kotlin integrada
// (2.2.10) y KSP 2.3.x ya se versiona de forma independiente a Kotlin.
plugins {
    id("com.android.application") version "9.3.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
}
