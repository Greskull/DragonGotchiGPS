# Filtro GPS anti-trampas

## La corrección clave sobre los "250 ft"

- 54 km/h equivalen a **15 m/s**, es decir **49,2 ft por segundo**.
- Si Android entrega una ubicación cada 1 segundo, el salto máximo legítimo es unos **15 m (≈49 ft)**.
- Si la entrega cada 5 segundos, el salto máximo es **75 m (≈246 ft)** — de ahí la cifra de 250 ft.

Por eso `GpsFilter` no usa una distancia fija entre *ticks*: calcula para cada punto

```text
distancia_máxima_permitida = velocidad_máxima (15 m/s) × segundos_entre_puntos
velocidad_implícita        = distancia_entre_puntos / segundos_entre_puntos
```

y rechaza el punto si la velocidad implícita supera los 15 m/s.

## Capas aplicadas, en orden

| # | Comprobación | Motivo | ¿Sospechoso? |
|---|---|---|---|
| 1 | `Location.isMock` (API 31+) / `isFromMockProvider` | Fake-GPS y apps de simulación | Sí |
| 2 | Precisión nula o > 30 m | Saltos urbanos, túneles, interior | No |
| 3 | Velocidad del chip GPS > 15 m/s | Vehículo detectado por el propio GPS | Sí |
| 4 | Velocidad implícita > 15 m/s (54 km/h) | Coche, bici muy rápida o teletransporte | Sí |
| 5 | Salto < 2 m | Ruido GPS estando parado (*jitter*) | No |
| 6 | Timestamp duplicado o hacia atrás | Datos corruptos / inyectados | No |

Detalles importantes:

- Cuando un punto se rechaza por velocidad, **la última posición válida no se actualiza**. Un tramposo no puede "arrastrar" su posición válida hasta donde quiera a base de teletransportes: tiene que volver a donde estaba.
- La distancia total se suma **solo** con puntos aceptados (`GpsFilter.totalDistanceM`), y la velocidad máxima guardada en la sesión también sale de puntos aceptados.
- Si hay al menos 5 puntos sospechosos y suponen más del 15 % del total (aceptados + sospechosos), la sesión se guarda con `isFlaggedSuspicious = true` y el historial la muestra con ⚠️.

## La defensa de fondo: la EXP no depende del GPS

La distancia se puede falsear; los pasos, mucho menos. Por eso:

- La **EXP sale del podómetro hardware** (`Sensor.TYPE_STEP_COUNTER`): 100 pasos = 1 EXP.
- Ir en coche apenas genera pasos de podómetro → no farmea niveles aunque el GPS se burlara.
- Solo si el móvil no tiene podómetro se estiman pasos por distancia (`distancia / 0,78 m`), y la sesión queda marcada como `stepsAreEstimated`.

## Ajuste de parámetros

Todos están en `GpsFilter` (companion object):

- `MAX_SPEED_MPS = 15.0` — súbelo a ~22 m/s si quieres permitir ciclismo rápido sin rechazos.
- `MAX_ACCURACY_M = 30f` — bájalo a 20 para más rigor (rechazará más en ciudad).
- `MIN_DISTANCE_M = 2.0` — ruido mínimo ignorado.

Hay tests de todos estos casos en `app/src/test/java/com/dragongotchi/app/LevelingAndGpsFilterTest.kt`.
