# 🐲 DragonGotchiGPS — Tu Strava con Tamagotchi dragón

> **Novedades v1.1.4:** 🐛 **bug de idioma en dialogos corregido**: el boton de confirmacion al finalizar actividad («Finalizar y tirar 🎲» / «Finalizar y descartar») salia en el idioma del TELEFONO, no en el de la app. Causa: el contenido de un AlertDialog se compone en su propia ventana, donde los recursos caen a los del sistema ignorando el contexto localizado; ahora los textos de los dialogos (finalizar actividad y eliminar ruta) se resuelven fuera, en la composicion de la pantalla, con el idioma elegido. 🚴 Y la actividad «Bici» acorta su nombre (antes Bicicleta) para que icono+texto quepan en una linea.
> **Novedades v1.1.3 (beta cerrada):** 🌍 **segundo idioma: inglés** — toda la interfaz extraída a recursos (`values/` = español, `values-en/` = inglés, 150+ claves incluidas notificaciones del servicio GPS y motivos de rechazo); ⚔️ **selector en Ajustes** bajo el consejo de batería y la versión, con estandartes 🇪🇸/🇬🇧 estilo guerra medieval (seleccionado = morado arcano + borde oro antiguo); 💾 la elección se guarda en el **mismo DataStore** que las recompensas y el cambio es **instantáneo sin recrear la actividad** (contexto localizado que envuelve a la Activity). 🐛 **Crash corregido**: al sustituir LocalContext por el contexto localizado a secas, `rememberLauncherForActivityResult` dejó de encontrar el ActivityResultRegistryOwner y la app moría al pulsar Entrenar (y habría muerto igual en el onboarding); ahora el contexto localizado es un wrapper sobre la Activity, se provee el owner explícitamente y los `as? Activity` pasan por `findActivity()` (también resucita «Abrir ajustes» del onboarding y «Opciones de privacidad»). 🔢 **Versionado público ligado al interno**: `1.<código÷10>.<código%10>` → código 13 = **1.1.3**, 14 = 1.1.4, 20 = 1.2.0, 21 = 1.2.1; el versionCode nunca se reinicia. Incluye todo lo de la anterior (historial máximo de 5 rutas).
> **Novedades v1.7.3:** 🗂️ **historial limitado a las últimas 5 rutas**: al guardarse una actividad nueva, la más antigua se elimina en la misma transacción (con sus puntos GPS, por la cascada de Room) para dejar sitio; nunca hay más de 5 y solo se borra cuando una nueva desplaza a la más antigua. Incluye tests de la regla de retención.
> **v1.7.2:** 🍪 **consentimiento GDPR/UMP integrado** (al arrancar se muestra el formulario de privacidad de Google si procede y AdMob solo se inicializa tras poder pedir anuncios; en Ajustes aparece «Opciones de privacidad» para cambiar la elección y la versión instalada), 🔋 **permiso `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` eliminado** (restringido en Europa): la app ya no abre ese ajuste; la bienvenida —una única vez— solo muestra un texto de consejo opcional, y 🎁 **box de dragones de Recompensas rehecho**: círculo a la izquierda, descripciones a todo el ancho restante y botón «Usar» en su propia línea inferior alineado a la derecha.
> **v1.7.1:** 🎁 **botones de dragón en Recompensas reubicados**: ahora van siempre **debajo de los textos**, al final de la tarjeta y alineados a la derecha (misma zona de pulsación que Aura/Shiny), sin deformar la tarjeta, 👑 **nombre de la app con placa heráldica** en Inicio (degradado morado arcano → sangre de dragón, borde de oro antiguo y letras en degradado dorado) y 📢 **anuncios reales de AdMob activados** con los IDs del usuario (editor `6505324142339269`).
> **v1.7.0:** 🗺️ **opción de mapa CARTO eliminada** (queda solo OpenStreetMap, 100% gratuito), 🛰️ **bug del punto GPS congelado corregido**: al iniciar una actividad nueva el servicio purga todo rastro de la sesión anterior (corta el flujo previo, estado a cero, ruta y filtro reseteados), ya no se siembra la posición con la caché `lastLocation` (devolvía el último punto de la sesión anterior) y solo se acepta como primer punto un fix nuevo posterior al inicio; la vista previa de posición solo se pinta en reposo y el mapa reinicia cámara/seguimiento con cada sesión (`sessionKey`), 📢 **banner de AdMob preparado** en la pantalla del mapa (componente `AdBanner`, SDK inicializado en la `Application`) con los **IDs oficiales de prueba** de Google en `res/values/strings.xml` (`admob_app_id` y `admob_banner_unit_id`): para activarlo solo hay que sustituir esos dos strings por los reales de la consola de AdMob, sin tocar código.
> **v1.6.0:** 📍 **la app pasa a llamarse DragonGotchiGPS** (nombre visible en el launcher, en Inicio, en la bienvenida y en la notificación de entrenamiento; el package `com.dragongotchi.app` NO cambia, así que se actualiza encima de la anterior sin perder datos), ⬅️ **botón Atrás destacado** en todas las pantallas secundarias: placa morado arcano con borde de oro antiguo, flecha dorada de 26 dp y la palabra «Atras» al lado, imposible de no ver en la esquina superior izquierda.
> **v1.5.0:** 🚴 **nueva actividad Bicicleta** (se elige al iniciar y queda registrada como tal en el historial; el resto de reglas no cambia), 🐉 **marcador del mapa = solo el sprite del dragón** con fondo transparente (sin círculo blanco, sin aura y siempre con el dragón/nivel vigente al pulsar Iniciar), 🎨 **tema mágico-medieval de Inicio extendido a todas las pantallas** (fondo de reino compartido `dragonKingdomBackground()` en Entrenar, Historial, Recompensas, Evoluciones y Ajustes), 🔘 botones **Recompensas/Evoluciones en una sola línea** (padding 6 dp, 13 sp, maxLines = 1, badge «!» reducido a 16 dp), 🛠️ **AGP 9.3.3 + Gradle 9.5.0** (Kotlin integrado: fuera el plugin kotlin-android; Compose plugin 2.2.10, KSP 2.3.6, Room 2.7.2, compileSdk 36), 🧹 **auditoría previa a compilar** de imports/variables sin usar (0 warnings de Kotlin) y 🛡️ **tarea `checkDuplicateResources`** ligada a `preBuild` que detecta recursos duplicados antes de que provoquen el error «Duplicate resources» (hoy: 0 duplicados).
> **v1.4.1 — correcciones tras prueba en móvil real:** 💥 **crash al iniciar una actividad eliminado** (la ruta y el dragón ya no usan `Marker`/`Polyline` de osmdroid: se pintan con overlays propios, inmunes al `MapViewRepository` anulado tras `onDetach`), 🗺️ **mapa reforzado** (el mapa se centra en tu posición nada más abrir Entrenar, el servicio siembra el primer punto al instante y, si un CDN de teselas no carga en tu red, el botón **«Capa: OSM/CARTO»** cambia a otro gratuito), 🛰️ **errores GPS visibles** (si la ubicación del dispositivo está apagada o falta el permiso, la app lo dice en vez de "buscar señal" eternamente) y 📍 **icono limpio** (fuera las ondas azules: solo el pin GPS rojo sobre el dragoncito).
> **v1.4 — assets finales:** 🐲 **90 sprites pixel-art 8-bit** (1056×1056 transparentes): 15 evoluciones del huevo al Dragón Ancestral para **tres dragones** (🟢 Esmeralda, 🔴 Escarlata y 🔵 Zafiro — estos dos son las mascotas desbloqueables del dice roll) **más sus 45 versiones SHINY doradas** y ✨ **aura** estilo anime en su hueco (detrás del dragón, 40 % de transparencia). Nombres y cómo sustituirlos: `docs/SPRITES.md`. (v1.3: mapa OSM corregido y ruta quemada · v1.2: dice roll · v1.1: mapa y marcador dragón.)

App Android en **Kotlin + Jetpack Compose** que graba tus rutas deportivas como Strava / Decathlon Coach (GPS en primer y segundo plano, historial, estadísticas en vivo y resumen final), con una mascota virtual: **un dragón que gana EXP y evoluciona en 15 niveles a medida que haces deporte**.

> 📦 **Cómo ejecutarla:** abre esta carpeta (`DragonGotchi`) en **Android Studio**, espera a que sincronice Gradle y pulsa ▶ Run en un móvil real (el GPS y el podómetro no funcionan bien en emulador). El mapa es **OpenStreetMap (osmdroid)**: gratuito y sin API key, solo necesita Internet para descargar las teselas (las ya visitadas quedan en caché).
>
> ✅ **Estado verificado:** el proyecto **compila sin errores ni warnings** (`:app:assembleDebug` → APK de ~12 MB) y los **8 tests unitarios pasan** (curva de niveles + filtro GPS). También tienes la APK de depuración ya generada (`DragonGotchi.apk`) para instalarla directamente en tu móvil y probarla sin Android Studio.

---

## ✅ Tus 5 claves, implementadas

### 1. Tracking GPS en primer plano y segundo plano + aviso de primera instalación
- El tracking vive en un **Foreground Service** (`LocationTrackingService`) con notificación persistente *"Entrenamiento en curso"*, `PARTIAL_WAKE_LOCK` y `START_STICKY`: sigue grabando con la app minimizada, la pantalla apagada o el móvil bloqueado, aunque Android cierre la Activity.
- **Primera vez que se instala** (`PermissionsOnboardingScreen`): pantalla de aviso con checklist guiado —
  1. Ubicación precisa, 2. Ubicación *"Permitir siempre"* (segundo plano), 3. Reconocimiento de actividad (pasos), 4. Notificaciones, 5. **Desactivar la optimización de batería** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + consejos para Samsung/Xiaomi/Huawei). Se refresca solo al volver de Ajustes.

### 2. Tracking fuera de Compose + historial y estadísticas
- **Compose solo pinta.** El servicio publica un `StateFlow<LiveTrackingState>` (`TrackingManager`) y la UI lo observa: si la UI muere, el entrenamiento no.
- La pantalla en vivo muestra **tiempo, distancia, ritmo (min/km), kcal, pasos y velocidad**, el mapa de la ruta en directo y el estado del GPS/filtro. Botones de **Pausar / Reanudar / Finalizar**.
- Al finalizar, la sesión y **todos sus puntos GPS aceptados** se guardan en **Room**. En **Historial** ves cada ruta con su mapa redibujado, tiempo, ritmo medio, kcal, pasos, velocidad máxima, EXP ganada y puntos GPS aceptados/rechazados. Puedes abrir el detalle y eliminar sesiones.
- **kcal** con METs según velocidad y tu peso (editable en Ajustes, 70 kg por defecto): `kcal = MET × peso × horas`, como las apps profesionales.

### 2b. Mapa gratuito + tu dragón como marcador (v1.1)
- Mapa real de **OpenStreetMap** vía **osmdroid** (`RouteMap.kt`): gratuito, sin API key, con caché de teselas en el móvil, atribución «© OpenStreetMap contributors» y zoom/pan táctil. En vivo sigue a tu dragón; si arrastras el mapa deja de seguirte y aparece «🐲 Centrar en mi dragón». En el historial encuadra la ruta completa.
- **Tu posición en el mapa es tu Tamagotchi**: al pulsar *Iniciar*, el nivel actual (el mismo que ves en «Mi dragón») se congela en la sesión (`dragonLevelAtStart` en el servicio) y su sprite se convierte en el marcador que se mueve contigo: huevo en nivel 1, huevo rompiéndose en nivel 2, etc.
- **Ruta quemada (scorched ground):** el recorrido ya no es una línea verde: son 5 capas (ceniza oscura translúcida → costra de carbón → brasa rojiza → lava naranja → chispas amarillas discontinuas), como si el dragón hubiera calcinado el suelo a su paso (`addScorchedRoute` en `RouteMap.kt`). El marcador de inicio es ahora morado arcano.
- **Escenario y aura:** en «Mi dragón» hay un único marco de 264 dp con borde dorado sobre fondo de castillo. Dentro comparten espacio dos imágenes: **detrás, el PNG del aura** (`reward_aura`, alpha 0,60 = **40 % de transparencia**, ocupando todo el marco) y **delante, siempre encima, el PNG del dragón**. Solo tienes que equipar el aura en Recompensas para verla.
- **Temática medieval 8-bit:** tipografía **Serif del sistema** en todos los textos (lo más gótico/medieval disponible en Compose sin añadir paquetes de fuentes), paleta de pergamino, morado arcano, oro antiguo y sangre de dragón (fondo «castillo a medianoche» en modo oscuro), formas casi rectas (2–8 dp), tarjetas con borde dorado, **barra de EXP** con marco oscuro y relleno dorado→brasa e **insignia de nivel** oscura con letras doradas («⚜ NIVEL X ⚜»).
- **Icono de la app:** el *foreground* adaptativo es `res/drawable/ic_launcher_foreground.png` (provisional pixel-art de 432×432). Para poner el tuyo, **sube tu PNG a `app/src/main/res/drawable/` con ese mismo nombre reemplazándolo**; no hay que tocar XML ni código. Recomendado: cuadrado, personaje centrado y con margen (zona segura de iconos adaptativos). El fondo del icono es el morado `#241536` (`launcher_bg`).
- **Tamaño siempre fijo: 48 dp** (`DRAGON_MARKER_SIZE_DP` en `DragonMarker.kt`). El sprite, sea vectorial o un PNG de 4000 px que pongas en `drawable`, se rasteriza una vez a un bitmap de 48×48 dp con círculo blanco y borde verde: el dragón gigante del nivel 15 **nunca tapará el mapa**.
- Pantallas secundarias (Evoluciones, detalle de ruta, Recompensas) con **botón «← Atrás»** reutilizable (`TopBars.kt`), además del gesto/botón Atrás de Android.

### 3. Fórmula de niveles (exacta a la pedida)
En `domain/LevelingSystem.kt` (con tests unitarios):
- **1 EXP cada 100 pasos** — pasos del **podómetro hardware** (`TYPE_STEP_COUNTER`, funciona en segundo plano sin apenas batería). El sobrante de 0–99 pasos se conserva entre sesiones. Si el móvil no tiene sensor, se estiman pasos por distancia (zancada 0,78 m) y la sesión lo indica.
- Nivel 1→2: **100 EXP**, y cada nivel cuesta **+50 %**: 100, 150, 225, 338, 506, 759, 1.139, 1.709, 2.563, 3.844, 5.767, 8.650, 12.975, 19.462 → **nivel 15 máximo**.

| Nivel | Forma | EXP al siguiente |
|---|---|---|
| 1 | 🥚 Huevo Místico | 100 |
| 2 | 🐣 Eclosión | 150 |
| 3 | Cría de Dragón | 225 |
| 4 | Dragoncito | 338 |
| 5 | Joven Dragón | 506 |
| 6 | Dragón Explorador | 759 |
| 7 | Dragón Veloz | 1.139 |
| 8 | Dragón Guerrero | 1.709 |
| 9 | Dragón Alado | 2.563 |
| 10 | 🐉 Dragón de Fuego | 3.844 |
| 11 | Dragón de Magma | 5.767 |
| 12 | Dragón Solar | 8.650 |
| 13 | Dragón Real | 12.975 |
| 14 | Dragón Titán | 19.462 |
| 15 | 👑 Dragón Ancestral Gigante | — (máximo) |

### 4. Sprites en `drawable` (placeholders listos para sustituir)
En `app/src/main/res/drawable/` tienes las **15 evoluciones** como vectoriales `stage_01_huevo_mistico.xml` … `stage_15_dragon_ancestral.xml`: del huevo al dragón gigante dorado con corona, creciendo en alas, cuernos, escamas y fuego. La pantalla **Evoluciones** muestra la galería con bloqueos 🔒.
👉 Los sprites finales ya están creados (v1.4): 90 PNG pixel-art en `res/drawable-nodpi/` — 15 evoluciones × 3 dragones (Esmeralda, Escarlata y Zafiro) y sus versiones shiny doradas, generados por `tools/generate_sprites.py`. Contrato de nombres y sustitución en `docs/SPRITES.md`.

### 5. Filtro GPS anti-trampas (`location/GpsFilter.kt`, con tests)
Tu idea era *"máx. ~54 km/h entre tics, unos 250 ft"*. Corrección importante: **54 km/h = 15 m/s ≈ 49 ft por segundo**. Los ~250 ft salen si el tick es de 5 s. Por eso el filtro no usa una distancia fija, sino **velocidad implícita = distancia ÷ tiempo entre puntos**, válido con cualquier intervalo. Capas, como en las apps profesionales:

1. **Ubicación simulada** (`isMock` / `isFromMockProvider`) → rechazada y marcada sospechosa.
2. **Precisión > 30 m** → rechazada (evita saltos falsos en ciudad/túneles).
3. **Velocidad del chip GPS > 15 m/s** → rechazada.
4. **Velocidad implícita entre puntos > 15 m/s (54 km/h)** → rechazada: en coche o con teletransportes de fake-GPS **no suma distancia**, y tu última posición válida no se mueve.
5. **Jitter < 2 m parado** → rechazado sin penalizar (ruido normal).
6. Timestamps duplicados/inválidos → rechazados.

Si ≥15 % de los puntos (mín. 5) son sospechosos, la sesión se guarda **marcada ⚠️ sospechosa**. Y la defensa definitiva: **la EXP sale de los pasos del hardware, no del GPS** — ir en coche no mueve el podómetro, así que no farmea niveles aunque alguien burle el GPS.

---

## 🗂 Arquitectura

```
app/src/main/java/com/dragongotchi/app/
├── MainActivity.kt / DragonGotchiApp.kt   (DI manual en AppContainer, sin Hilt)
├── location/
│   ├── LocationTrackingService.kt  ← Foreground Service: GPS + podómetro + timer + notificación
│   ├── GpsFilter.kt                ← filtro anti-trampas por capas
│   └── TrackingManager.kt          ← StateFlow que el servicio escribe y Compose solo lee
├── domain/
│   ├── LevelingSystem.kt           ← curva 100/150/225… x1.5, 15 DragonStage
│   └── StatsCalculator.kt          ← Haversine, ritmo, velocidad, kcal por METs
├── data/
│   ├── local/    Room: TrainingSessionEntity, RoutePointEntity, PetStateEntity
│   ├── repository/  PetRepository (pasos→EXP), SessionRepository
│   └── preferences/ DataStore: peso, nombre, onboarding
└── ui/  Compose + Navigation: onboarding, home (Tamagotchi), tracking en vivo,
         historial, detalle de ruta, evoluciones, ajustes
         components/RouteMap.kt = mapa OpenStreetMap (osmdroid) + marcador dragon
                      DragonMarker.kt = sprite -> bitmap fijo de 48 dp para el mapa
                      TopBars.kt = cabecera con boton "Atras" reutilizable
```

**Tests:** `app/src/test/.../LevelingAndGpsFilterTest.kt` — curva de EXP, caminata aceptada, coche rechazado a 200 km/h, mock/precisión rechazados, jitter parado ignorado.

## 🔋 Notas reales de Android que ya están contempladas
- Android 14+: permiso `FOREGROUND_SERVICE_LOCATION` y tipo `location` al hacer `startForeground`.
- Android 11+: el *"Permitir siempre"* no se puede pedir con diálogo; la app manda a Ajustes (por eso el paso 2 del onboarding es "Abrir ajustes").
- Android 13+: permiso de notificaciones. Android 10+: `ACTIVITY_RECOGNITION` para pasos.
- Sesiones mínimas para guardar: ≥10 s y (≥20 m o ≥50 pasos), para no llenar el historial de toques accidentales.

## 🚀 Ideas para la v2
- Sistema de Recompensas completo (medallas, rachas, cofres), segmentos y récords por ruta, retos semanales, alimentar/jugar con el dragón entre entrenos, exportar GPX, wearables (Health Connect), y sprites animados con Lottie.

## 🛠️ Notas de mantenimiento (normas del proyecto)
- **Toolchain fija:** AGP 9.3.3 + Gradle 9.5.0 + JDK 17. Con AGP 9, Kotlin va integrado: no volver a aplicar `org.jetbrains.kotlin.android`; el plugin de Compose (2.2.10) debe coincidir con el Kotlin integrado y KSP va en 2.3.x.
- **Antes de cada compilación:** revisar imports y variables sin usar (warnings del compilador Kotlin en el log de build) y dejar el build sin warnings `Unused`.
- **Anti «Duplicate resources»:** nunca dos ficheros con el mismo nombre y distinta extensión en la misma carpeta de `res/` (p. ej. `stage_01.png` + `stage_01.xml`), ni el mismo `name=""` repetido en `values/`. La tarea `checkDuplicateResources` (en `preBuild`) lo verifica automáticamente.
- Los avisos `UnusedResources` de lint sobre los sprites son **falsos positivos**: los PNG se resuelven por nombre en tiempo de ejecución (`dragonSpriteDrawableRes`), no se deben borrar.
