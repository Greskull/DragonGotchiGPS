# Sistema de Recompensas — Dice Roll

## 1. Filtro de actividad válida

Una actividad solo **cuenta** si cumple **ambas** condiciones a la vez:

| Condición | Mínimo |
|---|---|
| Pasos | **≥ 500 pasos** |
| Duración | **≥ 250 segundos** (4 min 10 s) |

Implementado en `ActivityFilter` (`domain/RewardsSystem.kt`) y aplicado en
`LocationTrackingService` al finalizar la sesión. Si no se cumple:

- ❌ No se guarda en el historial de rutas.
- ❌ No concede EXP a la mascota.
- ❌ No hay tirada de recompensas.

La app avisa en todo momento: en la pantalla de entrenamiento verás cuánto te
falta («pasos: 320/500 · tiempo: 2:30/4:10»), el diálogo de finalizar advierte
si la actividad se va a descartar, y al terminar un aviso confirma el resultado.

## 2. La tirada (dice roll)

Solo tras una actividad válida: **un número aleatorio del 1 al 100**
(`RewardsSystem.roll`). Cada recompensa tiene un **intervalo fijo y disjunto**,
así que es matemáticamente imposible que toquen dos a la vez.

> ⚠️ Nota sobre los intervalos: cada número de la tirada vale un 1 %.
> Un 15 % necesita **quince** números (1–15). Asignar «1–14» sería un 14 %.
> Por eso, respetando los porcentajes pedidos, los intervalos son:

| Números | Recompensa | % |
|---|---|---|
| **1 – 15** | ✨ Aura de dragón | 15 % |
| **16 – 22** | 🌟 Sprite Shiny (skin dorada) | 7 % |
| **23 – 25** | 💚 Dragón 2 · Esmeralda | 3 % |
| **26 – 28** | 💜 Dragón 3 · Amatista | 3 % |
| 29 – 100 | Sin recompensa | 72 % |

### Anulación al desbloquear

Cuando desbloqueas una recompensa, **su intervalo queda muerto para siempre**:

- Si otra tirada cae en él, no toca nada (no se repite la tirada ni se pasa
  el premio a la siguiente recompensa).
- Una recompensa jamás puede desbloquearse dos veces.
- La probabilidad real de premio baja: 28 % al principio → 0 % con todo
  desbloqueado. El menú de Recompensas muestra el % actual.

## 3. Menú de Recompensas

Botón «🎁 Recompensas» en Inicio (donde antes estaba Historial):

- **¡Aviso de novedad!** Si una tirada desbloquea algo, el botón muestra un
  **círculo rojo con «!»** hasta que entras en el menú.
- **Aura**: botón Activar/Desactivar (ON/OFF). Se ve tras tu dragón en
  «Mi dragón» y como anillo dorado en el marcador del mapa.
- **Sprite Shiny**: botón Activar/Desactivar (ON/OFF). Tiñe el sprite de
  dorado con destellos, en «Mi dragón» y en el mapa.
- **Dragón 1 / 2 / 3**: botón «Usar Dragón N». El 1 siempre está disponible;
  el 2 (Esmeralda) y el 3 (Amatista) exigen haberlos ganado en la tirada.
  De momento son variantes de color del sprite; cuando tengas los assets
  propios de cada dragón, se conectan en `spriteColorFilter` /
  `createDragonMarkerDrawable`.

Todo el estado (desbloqueos, equipamiento, mascota elegida, total de tiradas
y última tirada) persiste en DataStore (`UserPreferences.rewardState`), sin
tocar la base de datos de rutas: actualizar la app no borra tu historial.

## 4. Assets pendientes (cuando los tengas)

- **Aura**: guarda `reward_aura.png` en `res/drawable/` y borra
  `reward_aura.xml`. Sin tocar código.
- **Dragones 2 y 3**: cuando existan sus sprites por nivel, el sitio natural
  es resolver el drawable según `(selectedPetId, level)` junto a
  `DragonStage`, manteniendo tinte Shiny y tamaño fijo de mapa (48 dp).

## 5. Añadir recompensas en el futuro

Asignar el **siguiente bloque libre desde el 29**, en orden ascendente
(29–31, 32–34, …), sin modificar los intervalos ya publicados: así las
probabilidades de los jugadores que ya desbloquearon cosas no cambian
retroactivamente. Cada recompensa nueva = una entrada en `RewardId` con su
`rollRange` y su `probabilityPercent` (deben coincidir: rango de 3 números
= 3 %), un botón en el menú y, si se equipa, su rama en el estado.

## Tests

`RewardsAndActivityFilterTest.kt`: límites exactos del filtro (499/500 pasos,
249/250 s), límites de cada intervalo (1/15, 16/22, 23/25, 26/28, 29/100),
no solapamiento entre intervalos, anulación tras desbloquear y rango 1–100
de la tirada (2.000 tiradas con semilla fija).
