# Sprites y assets de DragonGotchi (v1.4)

Todos los assets son **PNG de 1056×1056 px con fondo transparente**
(1056 = 264 dp × 4: el tamaño exacto del marco `PetStageSize` de la
pantalla principal), en **estilo pixel-art 8-bit**. Viven en
`app/src/main/res/drawable-nodpi/` para que Android no los reescale por
densidad: la app los escala a cada uso (marco de 264 dp, miniaturas de
64 dp en Evoluciones y marcador de 48 dp en el mapa).

## Los 3 dragones

| Dragón | Color | Cómo se consigue |
|---|---|---|
| 🟢 **Dragón Esmeralda** | Verde | Dragón inicial (siempre disponible) |
| 🔴 **Dragón Escarlata** | Rojo fuego | Recompensa del dice roll (números 23–25, 3 %) |
| 🔵 **Dragón Zafiro** | Azul escarcha | Recompensa del dice roll (números 26–28, 3 %) |

Cada uno tiene su propia línea de **15 evoluciones**: huevo → eclosión →
cría → hatchling que crece en cuerpo, alas, cuernos, garras y espinas →
adulto que escupe fuego (Nv. 10) → dragón real con corona (Nv. 13) →
titán → **Dragón Ancestral** gigante (Nv. 15).

## Versiones SHINY

Los mismos 45 sprites con **solo el color principal cambiado a dorado**
(las alas conservan su color, como mandan las normas shiny). Al equipar
la recompensa 🌟 la app usa directamente estos PNG: ya no hay filtros de
color en tiempo de ejecución.

## Contrato de nombres (lo usa `dragonSpriteAssetName`)

```
Dragón 1 normal : stage_03_cria_dragon.png        (stage_XX_<etapa>)
Dragón 1 shiny  : shiny_stage_03_cria_dragon.png
Dragón 2 normal : pet2_stage_03.png
Dragón 2 shiny  : shiny_pet2_stage_03.png
Dragón 3 normal : pet3_stage_03.png
Dragón 3 shiny  : shiny_pet3_stage_03.png
Aura            : reward_aura.png   (detrás del dragón, alpha 0,60 en app)
```

Con caídas en cascada: si falta un shiny cae al normal, si falta un dragón
cae al Dragón 1, y en último término al huevo inicial.

## Aura

`reward_aura.png`: anillo de llamas doradas/naranjas estilo anime
(inspirado en las auras de energía clásicas), con el centro transparente
para que el dragón se vea delante. La app la pinta **detrás** del sprite,
en el mismo marco de 264 dp y con **40 % de transparencia** (alpha 0,60).
En el mapa se representa como anillo dorado del marcador.

## Icono de la app

`res/drawable/ic_launcher_foreground.png` (432×432): el dragoncito
Esmeralda con un **pin GPS rojo y ondas de señal sobre la cabeza**, para
que el icono ya cuente la función de tracking. El conjunto está centrado
en la zona segura de los iconos adaptativos.

## Regenerar o sustituir assets

- Los genera `tools/generate_sprites.py` (PIL, rejilla 96×96 escalada
  con NEAREST): `python3 tools/generate_sprites.py`. Cambia paletas y
  formas ahí y todo se regenera coherente (normales + shiny + aura).
- ¿Tienes arte final propio? Sustituye cada PNG por el tuyo **con el
  mismo nombre y carpeta** (1056×1056 transparente recomendado) y listo:
  no hay que tocar código. Lo mismo aplica al aura y al icono
  (`ic_launcher_foreground.png` en `res/drawable/`).

Vista previa de las 90 láminas: `docs/sprites-preview.jpg`.
Aura + dragón combinados: `docs/aura-preview.jpg`.
