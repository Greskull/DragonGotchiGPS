#!/usr/bin/env python3
"""
Generador de assets pixel-art de DragonGotchi (8-bit retro).

Genera en app/src/main/res/drawable-nodpi/:
  - 15 evoluciones x 3 dragones (Esmeralda, Escarlata, Zafiro) = 45 PNG
  - Las mismas 45 en version SHINY (color principal -> dorado) = 45 PNG
  - reward_aura.png (aura estilo anime, fondo transparente)

Todos los PNG son 1056x1056 (264 dp x 4, el hueco PetStageSize de la
pantalla principal) con fondo transparente, dibujados en una rejilla
de 96x96 y escalados con NEAREST para mantener el pixel crujiente.
Estan en drawable-nodpi para que Android NO los reescale por densidad:
la app los escala ella misma a cada tamano (264, 64 y 48 dp).

Uso:  python3 tools/generate_sprites.py
"""
from PIL import Image, ImageDraw
import math
import os

GRID = 96
SCALE = 11  # 96 x 11 = 1056 px
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable-nodpi")
OUT = os.path.abspath(OUT)

SUFFIXES = [
    "huevo_mistico", "eclosion", "cria_dragon", "dragoncito", "joven_dragon",
    "dragon_explorador", "dragon_veloz", "dragon_guerrero", "dragon_alado",
    "dragon_fuego", "dragon_magma", "dragon_solar", "dragon_real",
    "dragon_titan", "dragon_ancestral",
]

# Paleta por dragon: color principal y derivados.
DRAGONS = {
    1: dict(name="esmeralda", main=(46, 181, 84), dark=(18, 110, 52), light=(128, 230, 148),
            belly=(255, 240, 179), wing=(24, 128, 66), membrane=(158, 240, 174),
            horn=(255, 224, 130), accent=(0, 191, 165)),
    2: dict(name="escarlata", main=(224, 66, 52), dark=(150, 24, 24), light=(255, 140, 112),
            belly=(255, 227, 179), wing=(176, 28, 28), membrane=(255, 171, 145),
            horn=(255, 236, 179), accent=(255, 109, 0)),
    3: dict(name="zafiro", main=(44, 122, 224), dark=(20, 66, 150), light=(130, 196, 255),
            belly=(227, 242, 253), wing=(24, 84, 176), membrane=(144, 202, 249),
            horn=(225, 245, 254), accent=(64, 196, 255)),
}
SHINY_MAIN = dict(main=(255, 205, 48), dark=(204, 138, 0), light=(255, 238, 140))
OUTLINE = (24, 16, 28, 255)


def shiny_palette(base):
    p = dict(base)
    p.update(SHINY_MAIN)  # solo cambia el color principal (y sus sombras)
    return p


def C(rgb, a=255):
    return (rgb[0], rgb[1], rgb[2], a)


def draw_dragon(img, stage, pal):
    d = ImageDraw.Draw(img)
    cx = 48
    ground = 90

    def ell(box, fill, outline=OUTLINE, w=2):
        d.ellipse(box, fill=fill, outline=outline, width=w)

    def poly(points, fill, outline=OUTLINE, w=2):
        d.polygon(points, fill=fill)
        if outline:
            d.line(points + [points[0]], fill=outline, width=w, joint="curve")

    def flame(cx0, cy0, s, colors):
        poly([(cx0, cy0 - s), (cx0 + s * 0.55, cy0 + s * 0.25), (cx0 + s * 0.2, cy0 + s * 0.1),
              (cx0 + s * 0.3, cy0 + s * 0.75), (cx0, cy0 + s * 0.45), (cx0 - s * 0.3, cy0 + s * 0.75),
              (cx0 - s * 0.2, cy0 + s * 0.1), (cx0 - s * 0.55, cy0 + s * 0.25)], colors[0], None)
        poly([(cx0, cy0 - s * 0.55), (cx0 + s * 0.3, cy0 + s * 0.25), (cx0, cy0 + s * 0.5),
              (cx0 - s * 0.3, cy0 + s * 0.25)], colors[1], None)

    # ---------------- Etapa 1: huevo ----------------
    if stage == 1:
        ell([cx - 21, 18, cx + 21, 84], C(pal["light"]))
        ell([cx - 21, 18, cx + 21, 84], None, OUTLINE, 3)
        for (x, y, r) in [(38, 38, 5), (58, 52, 6), (44, 66, 5), (56, 30, 4)]:
            ell([x - r, y - r, x + r, y + r], C(pal["main"]))
        # brillo pixel
        d.rectangle([cx - 12, 28, cx - 8, 40], fill=(255, 255, 255, 150))
        # base de hierba/sombra
        ell([cx - 24, 82, cx + 24, 90], (0, 0, 0, 70))
        return

    growth = max(0.0, (stage - 3) / 12.0)  # 0 (cria) .. 1 (ancestral)

    bw = 23 + growth * 19       # ancho cuerpo 23..42
    bh = 27 + growth * 21       # alto cuerpo 27..48
    hr = 11 + growth * 6        # radio cabeza 11..17
    body_top = ground - bh
    head_cy = body_top - hr + 7
    wing_w = 0 if stage < 3 else 9 + growth * 25
    wing_h = 0 if stage < 3 else 11 + growth * 25
    horn_l = 4 + growth * 9

    # sombra
    ell([cx - bw * 0.62, ground - 3, cx + bw * 0.62, ground + 4], (0, 0, 0, 70))

    # ---------------- Cola (detras) ----------------
    if stage >= 3:
        tail_len = 13 + growth * 15
        poly([(cx + bw * 0.28, ground - 12), (cx + bw * 0.28 + tail_len, ground - 7),
              (cx + bw * 0.28 + tail_len - 3, ground - 1), (cx + bw * 0.2, ground - 2)], C(pal["dark"]))
        # pincho de la cola
        tipx = cx + bw * 0.28 + tail_len
        poly([(tipx, ground - 11), (tipx + 6, ground - 5), (tipx, ground - 1)], C(pal["horn"]))

    # ---------------- Alas (detras del cuerpo) ----------------
    if stage >= 3:
        for side in (-1, 1):
            sx = cx + side * bw * 0.26
            sy = body_top + 10
            tipx = sx + side * wing_w
            tipy = sy - wing_h
            # membrana con borde festoneado tipo murcielago
            pts = [(sx, sy),
                   (tipx, tipy),
                   (tipx - side * wing_w * 0.18, sy - wing_h * 0.38),
                   (tipx - side * wing_w * 0.05, sy - wing_h * 0.12),
                   (sx + side * wing_w * 0.28, sy - wing_h * 0.30),
                   (sx + side * wing_w * 0.18, sy + 2)]
            poly(pts, C(pal["wing"]))
            inner = [(x + (cx - x) * 0.22, y + 2) for (x, y) in pts]
            poly(inner, C(pal["membrane"]), None)

    # ---------------- Patas y garras ----------------
    if stage >= 3:
        for side in (-1, 1):
            fx = cx + side * bw * 0.34
            ell([fx - 6, ground - 7, fx + 6, ground + 1], C(pal["dark"]))
            if stage >= 7:  # garras
                for t in (-4, 0, 4):
                    poly([(fx + t - 1, ground), (fx + t + 1, ground), (fx + t, ground + 4)], C(pal["belly"]), None)

    # ---------------- Cuerpo ----------------
    if stage >= 3:
        ell([cx - bw / 2 - 2, body_top - 2, cx + bw / 2 + 2, ground + 2], OUTLINE)
        ell([cx - bw / 2, body_top, cx + bw / 2, ground], C(pal["main"]))
        # barriga
        ell([cx - bw * 0.30, body_top + bh * 0.30, cx + bw * 0.30, ground - 3], C(pal["belly"]))
        for i in range(1, 5):
            y = body_top + bh * 0.30 + i * (bh * 0.66 / 5)
            d.line([(cx - bw * 0.26, y), (cx + bw * 0.26, y)], fill=C(pal["dark"], 120), width=1)
        # brazos
        for side in (-1, 1):
            ax = cx + side * (bw / 2 + 1)
            ay = body_top + bh * 0.45
            d.line([(ax, ay), (ax + side * 4, ay + 9)], fill=C(pal["dark"]), width=4)
            if stage >= 8:
                for t in range(3):
                    d.line([(ax + side * 4 + (t - 1), ay + 9), (ax + side * 6 + (t - 1), ay + 12)],
                           fill=C(pal["belly"]), width=1)

    # ---------------- Cabeza ----------------
    hx, hy = cx, head_cy
    ell([hx - hr - 2, hy - hr - 2, hx + hr + 2, hy + hr + 2], OUTLINE)
    ell([hx - hr, hy - hr, hx + hr, hy + hr], C(pal["main"]))
    # hocico
    ell([hx - hr * 0.62, hy + hr * 0.12, hx + hr * 0.62, hy + hr * 0.82], C(pal["belly"]))
    d.point([(hx - 3, hy + hr * 0.45), (hx + 3, hy + hr * 0.45)], fill=OUTLINE)
    d.rectangle([hx - 4, hy + hr * 0.44, hx - 3, hy + hr * 0.52], fill=OUTLINE)
    d.rectangle([hx + 3, hy + hr * 0.44, hx + 4, hy + hr * 0.52], fill=OUTLINE)
    # dientes en adultos
    if stage >= 6:
        poly([(hx - 5, hy + hr * 0.68), (hx - 2, hy + hr * 0.68), (hx - 3.5, hy + hr * 0.88)], (255, 255, 255, 255), None)
        poly([(hx + 2, hy + hr * 0.68), (hx + 5, hy + hr * 0.68), (hx + 3.5, hy + hr * 0.88)], (255, 255, 255, 255), None)
    # ojos
    for side in (-1, 1):
        ex = hx + side * hr * 0.42
        ey = hy - hr * 0.12
        r = 2.6 + growth * 0.9
        ell([ex - r, ey - r, ex + r, ey + r], (255, 255, 255, 255), None)
        ell([ex - r * 0.5, ey - r * 0.4, ex + r * 0.5, ey + r * 0.7], OUTLINE, None)
        d.rectangle([ex - 1, ey - 2, ex, ey - 1], fill=(255, 255, 255, 255))
        if stage >= 8:  # ceja fiera
            d.line([(ex - r, ey - r - 2), (ex + r * 0.7, ey - r - 1)], fill=C(pal["dark"]), width=2)
    # cuernos (crecen con el nivel)
    for side in (-1, 1):
        bx = hx + side * hr * 0.55
        by = hy - hr * 0.78
        poly([(bx - 3, by + 3), (bx + 3, by + 3), (bx + side * horn_l * 0.45, by - horn_l)], C(pal["horn"]))
        if stage >= 8:  # cuernos secundarios
            bx2 = hx + side * hr * 0.92
            poly([(bx2 - 2, by + 7), (bx2 + 2, by + 7), (bx2 + side * horn_l * 0.4, by - horn_l * 0.45)], C(pal["horn"]))
    # aletas de las orejas
    for side in (-1, 1):
        ex = hx + side * hr
        poly([(ex, hy - 3), (ex + side * (5 + growth * 4), hy + 1), (ex, hy + 5)], C(pal["accent"]))
    # espinas dorsales
    if stage >= 5:
        n = 3 + int(growth * 2)
        for i in range(n):
            t = i / max(1, n - 1)
            sx = hx - hr * 0.5 + t * hr
            sy = hy - hr - 1 + t * 3
            h = 4 + growth * 4
            poly([(sx - 2, sy), (sx + 2, sy), (sx, sy - h)], C(pal["dark"]), None)
    # fuego al respirar (adultos)
    if stage >= 10:
        fx = hx + hr + 7
        fy = hy + hr * 0.45
        flame(fx, fy, 7 + growth * 5, [C(pal["accent"]), (255, 224, 130, 255)])
    # corona real
    if stage >= 13:
        cw = hr * 1.15
        poly([(hx - cw / 2, hy - hr - 4), (hx - cw / 2 + 3, hy - hr - 10), (hx - cw / 4, hy - hr - 5),
              (hx, hy - hr - 12), (hx + cw / 4, hy - hr - 5), (hx + cw / 2 - 3, hy - hr - 10),
              (hx + cw / 2, hy - hr - 4)], (255, 193, 7, 255))
        d.rectangle([hx - 1, hy - hr - 8, hx + 1, hy - hr - 6], fill=C(pal["accent"]))
    # destellos del ancestral
    if stage == 15:
        for (x, y) in [(10, 16), (84, 12), (14, 46), (86, 44)]:
            d.line([(x - 3, y), (x + 3, y)], fill=(255, 224, 130, 255), width=2)
            d.line([(x, y - 3), (x, y + 3)], fill=(255, 224, 130, 255), width=2)

    # ---------------- Etapa 2: eclosion (cascaron delante) ----------------
    if stage == 2:
        # cascaron roto en la base, delante del cuerpo recien nacido
        shell = [(cx - 24, 62), (cx - 17, 56), (cx - 10, 63), (cx - 3, 57), (cx + 4, 64),
                 (cx + 11, 58), (cx + 18, 64), (cx + 24, 59), (cx + 26, 86), (cx - 26, 86)]
        poly(shell, C(pal["light"]))
        for (x, y, r) in [(36, 74, 4), (58, 78, 5), (48, 68, 3)]:
            ell([x - r, y - r, x + r, y + r], C(pal["main"]), None)
        # trozos de cascara volando
        poly([(20, 40), (26, 37), (25, 43)], C(pal["light"]))
        poly([(72, 34), (78, 37), (71, 40)], C(pal["light"]))


def make_sprite(stage, pal):
    img = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
    draw_dragon(img, stage, pal)
    return img.resize((GRID * SCALE, GRID * SCALE), Image.NEAREST)


def make_aura():
    """Aura de llamas estilo anime: anillo de fuego con centro transparente."""
    img = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 48, 52
    layers = [
        # (radio_x, radio_y, n_llamas, largo, color) de fuera a dentro
        (34, 38, 14, 15, (255, 109, 0, 255)),    # fuego exterior naranja
        (31, 35, 14, 12, (255, 167, 38, 255)),   # oro oscuro
        (28, 32, 12, 10, (255, 213, 79, 255)),   # dorado
        (25, 29, 12, 8, (255, 241, 160, 255)),   # nucleo palido
    ]
    for (rx, ry, n, length, color) in layers:
        for i in range(n):
            ang = (i / n) * math.tau - math.pi / 2
            # Llamas irregulares y ascendentes (estilo anime): las de
            # arriba son mucho mas largas, las de los pies cortas, y cada
            # una tiene una longitud distinta pseudo-aleatoria estable.
            up = 1.45 if math.sin(ang) < -0.3 else (0.5 if math.sin(ang) > 0.55 else 0.85)
            jitter = 0.75 + ((i * 37) % 5) * 0.14
            bx = cx + math.cos(ang) * rx
            by = cy + math.sin(ang) * ry
            tipx = cx + math.cos(ang) * (rx + length * up * jitter)
            tipy = cy + math.sin(ang) * (ry + length * up * jitter)
            side = 5.5
            px = math.cos(ang + math.pi / 2) * side
            py = math.sin(ang + math.pi / 2) * side
            d.polygon([(bx + px, by + py), (tipx, tipy), (bx - px, by - py)], fill=color)
    # resplandor central suave (detras del dragon, apenas un halo)
    glow = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([cx - 24, cy - 28, cx + 24, cy + 28], fill=(255, 183, 77, 38))
    img = Image.alpha_composite(glow, img)
    return img.resize((GRID * SCALE, GRID * SCALE), Image.NEAREST)


def asset_name(pet_id, stage, shiny):
    suffix = SUFFIXES[stage - 1]
    if pet_id == 1:
        return f"{'shiny_' if shiny else ''}stage_{stage:02d}_{suffix}.png"
    return f"{'shiny_' if shiny else ''}pet{pet_id}_stage_{stage:02d}.png"


def main():
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for pet_id, base in DRAGONS.items():
        for shiny in (False, True):
            pal = shiny_palette(base) if shiny else base
            for stage in range(1, 16):
                sprite = make_sprite(stage, pal)
                sprite.save(os.path.join(OUT, asset_name(pet_id, stage, shiny)), optimize=True)
                total += 1
    make_aura().save(os.path.join(OUT, "reward_aura.png"), optimize=True)
    total += 1
    print(f"{total} assets generados en {OUT}")


if __name__ == "__main__":
    main()
