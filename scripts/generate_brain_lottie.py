#!/usr/bin/env python3
"""Generate 5 brain mascot Lottie JSON files (512x512, 60fps, loopable)."""

import json
import math
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"
W, H, FR = 512, 512, 60
CX, CY = 256, 256


def hex_rgb(h: str) -> list[float]:
    h = h.lstrip("#")
    return [int(h[i : i + 2], 16) / 255.0 for i in (0, 2, 4)]


def static(k):
    return {"a": 0, "k": k}


def animated(keyframes):
    return {"a": 1, "k": keyframes}


def anim_property(value, default):
    """Accept static value, keyframe list, or pre-built {a,k} Lottie property."""
    if isinstance(value, dict) and "a" in value and "k" in value:
        return value
    if isinstance(value, list) and value and isinstance(value[0], dict):
        return animated(value)
    if value is None:
        return static(default)
    return static(value)


def transform(
    pos=None,
    scale=None,
    rotation=None,
    opacity=100,
    anchor=None,
):
    anchor = anchor or [0, 0, 0]
    return {
        "o": anim_property(opacity, 100),
        "r": anim_property(rotation, 0),
        "p": anim_property(pos, [CX, CY, 0]),
        "a": static(anchor),
        "s": anim_property(scale, [100, 100, 100]),
        "sk": static(0),
        "sa": static(0),
    }


def shape_layer(ind, name, shapes, ks, ip=0, op=180, parent=None):
    layer = {
        "ddd": 0,
        "ind": ind,
        "ty": 4,
        "nm": name,
        "sr": 1,
        "ks": ks,
        "ao": 0,
        "shapes": shapes,
        "ip": ip,
        "op": op,
        "st": 0,
        "bm": 0,
    }
    if parent is not None:
        layer["parent"] = parent
    return layer


def group(items, name="Group"):
    return {
        "ty": "gr",
        "it": items
        + [
            {
                "ty": "tr",
                "p": static([0, 0]),
                "a": static([0, 0]),
                "s": static([100, 100]),
                "r": static(0),
                "o": static(100),
                "sk": static(0),
                "sa": static(0),
                "nm": "Transform",
            }
        ],
        "nm": name,
        "np": len(items) + 1,
        "cix": 2,
        "bm": 0,
        "ix": 1,
        "mn": "ADBE Vector Group",
        "hd": False,
    }


def ellipse(w, h, color, fill_opacity=100, stroke=None):
    items = [
        {
            "ty": "el",
            "p": static([0, 0]),
            "s": static([w, h]),
            "nm": "Ellipse",
            "mn": "ADBE Vector Shape - Ellipse",
            "hd": False,
        },
        {
            "ty": "fl",
            "c": static(color + [1]),
            "o": static(fill_opacity),
            "r": 1,
            "bm": 0,
            "nm": "Fill",
            "mn": "ADBE Vector Graphic - Fill",
            "hd": False,
        },
    ]
    if stroke:
        sw, sc, so = stroke
        items.insert(
            1,
            {
                "ty": "st",
                "c": static(sc + [1]),
                "o": static(so),
                "w": static(sw),
                "lc": 2,
                "lj": 2,
                "ml": 4,
                "bm": 0,
                "nm": "Stroke",
                "mn": "ADBE Vector Graphic - Stroke",
                "hd": False,
            },
        )
    return group(items)


def rect(w, h, color, radius=0):
    return group(
        [
            {
                "ty": "rc",
                "p": static([0, 0]),
                "s": static([w, h]),
                "r": static(radius),
                "nm": "Rect",
                "mn": "ADBE Vector Shape - Rect",
                "hd": False,
            },
            {
                "ty": "fl",
                "c": static(color + [1]),
                "o": static(100),
                "r": 1,
                "bm": 0,
                "nm": "Fill",
                "mn": "ADBE Vector Graphic - Fill",
                "hd": False,
            },
        ]
    )


def path(vertices, in_tangents, out_tangents, closed, color, stroke_w=0, stroke_color=None):
    items = [
        {
            "ty": "sh",
            "ks": static(
                {
                    "i": in_tangents,
                    "o": out_tangents,
                    "v": vertices,
                    "c": closed,
                }
            ),
            "nm": "Path",
            "mn": "ADBE Vector Shape - Group",
            "hd": False,
        }
    ]
    if stroke_w > 0:
        items.append(
            {
                "ty": "st",
                "c": static((stroke_color or color) + [1]),
                "o": static(100),
                "w": static(stroke_w),
                "lc": 2,
                "lj": 2,
                "ml": 4,
                "bm": 0,
                "nm": "Stroke",
                "mn": "ADBE Vector Graphic - Stroke",
                "hd": False,
            }
        )
    else:
        items.append(
            {
                "ty": "fl",
                "c": static(color + [1]),
                "o": static(100),
                "r": 1,
                "bm": 0,
                "nm": "Fill",
                "mn": "ADBE Vector Graphic - Fill",
                "hd": False,
            }
        )
    return group(items)


def line(x1, y1, x2, y2, color, width=2, opacity_kf=None):
    verts = [[x1, y1], [x2, y2]]
    zero = [[0, 0], [0, 0]]
    stroke_op = opacity_kf if opacity_kf else static(100)
    return group(
        [
            {
                "ty": "sh",
                "ks": static({"i": zero, "o": zero, "v": verts, "c": False}),
                "nm": "Line",
                "mn": "ADBE Vector Shape - Group",
                "hd": False,
            },
            {
                "ty": "st",
                "c": static(color + [1]),
                "o": stroke_op,
                "w": static(width),
                "lc": 2,
                "lj": 2,
                "ml": 4,
                "bm": 0,
                "nm": "Stroke",
                "mn": "ADBE Vector Graphic - Stroke",
                "hd": False,
            },
        ]
    )


def breathe_scale(op, amount=5, speed_frames=None):
    speed = speed_frames or op
    mid = speed // 2
    return animated(
        [
            {"t": 0, "s": [100, 100, 100]},
            {"t": mid, "s": [100 + amount, 100 + amount, 100]},
            {"t": speed, "s": [100, 100, 100]},
        ]
    )


def pulse_opacity(op, low=30, high=100):
    return animated(
        [
            {"t": 0, "s": [high]},
            {"t": op // 2, "s": [low]},
            {"t": op, "s": [high]},
        ]
    )


def sway_position(op, amount=16):
    return animated(
        [
            {"t": 0, "s": [CX, CY, 0]},
            {"t": op // 4, "s": [CX + amount, CY, 0]},
            {"t": op // 2, "s": [CX, CY, 0]},
            {"t": 3 * op // 4, "s": [CX - amount, CY, 0]},
            {"t": op, "s": [CX, CY, 0]},
        ]
    )


def shake_rotation(op, interval=12, degrees=8):
    kf = [{"t": 0, "s": [0]}]
    t = 0
    sign = 1
    while t < op:
        kf.append({"t": t, "s": [degrees * sign]})
        t += interval // 2
        sign *= -1
    kf.append({"t": op, "s": [0]})
    return animated(kf)


def shake_position(op, interval=12, amount=10):
    kf = [{"t": 0, "s": [CX, CY, 0]}]
    t = 0
    dirs = [(1, 0), (0, 1), (-1, 1), (1, -1), (-1, 0), (0, -1)]
    i = 0
    while t < op:
        dx, dy = dirs[i % len(dirs)]
        kf.append({"t": t, "s": [CX + dx * amount, CY + dy * amount, 0]})
        t += interval
        i += 1
    kf.append({"t": op, "s": [CX, CY, 0]})
    return animated(kf)


def brain_body_path():
    """Rounded brain silhouette centered at origin."""
    v = [
        [0, -90],
        [70, -70],
        [85, 20],
        [55, 75],
        [0, 85],
        [-55, 75],
        [-85, 20],
        [-70, -70],
    ]
    z = [[0, 0]] * 8
    return path(v, z, z, True, [0.5, 0.5, 0.5])


def make_comp(name, op, layers):
    return {
        "v": "5.7.4",
        "fr": FR,
        "ip": 0,
        "op": op,
        "w": W,
        "h": H,
        "nm": name,
        "ddd": 0,
        "assets": [],
        "layers": layers,
        "markers": [],
    }


def neural_layers(ind_start, op, color, flicker=False, dim=False, erratic=False):
    lines = [
        (-40, -20, 30, -30),
        (30, -30, 50, 10),
        (-50, 10, -10, 30),
        (10, 30, 55, 25),
        (-30, 40, 20, 50),
    ]
    layers = []
    white = [1, 1, 1]
    for i, (x1, y1, x2, y2) in enumerate(lines):
        if erratic:
            op_kf = animated(
                [
                    {"t": 0, "s": [100]},
                    {"t": 8, "s": [20]},
                    {"t": 16, "s": [90]},
                    {"t": 24, "s": [15]},
                    {"t": 32, "s": [100]},
                    {"t": op, "s": [100]},
                ]
            )
        elif flicker:
            op_kf = animated(
                [
                    {"t": 0, "s": [80]},
                    {"t": 15, "s": [35]},
                    {"t": 30, "s": [75]},
                    {"t": op, "s": [80]},
                ]
            )
        elif dim:
            op_kf = pulse_opacity(op, 15, 45)
        else:
            op_kf = pulse_opacity(op, 40, 100)
        layers.append(
            shape_layer(
                ind_start + i,
                f"Neural {i}",
                [line(x1, y1, x2, y2, white, 2, op_kf)],
                transform(pos=[CX, CY, 0], opacity=op_kf),
                op=op,
            )
        )
    return layers


def sparkle_layers(ind_start, op, positions):
    layers = []
    for i, (ox, oy) in enumerate(positions):
        op_kf = animated(
            [
                {"t": 0, "s": [0]},
                {"t": 20 + i * 8, "s": [100]},
                {"t": 40 + i * 8, "s": [0]},
                {"t": op, "s": [0]},
            ]
        )
        layers.append(
            shape_layer(
                ind_start + i,
                f"Sparkle {i}",
                [ellipse(8, 8, [1, 1, 1])],
                transform(pos=[CX + ox, CY + oy, 0], scale=[100, 100, 100], opacity=op_kf),
                op=op,
            )
        )
    return layers


def build_healthy():
    op = 180
    pink = hex_rgb("FCA5A5")
    dark = hex_rgb("1E1E2E")
    layers = []

    layers.append(
        shape_layer(
            1,
            "Glow",
            [ellipse(280, 300, pink, fill_opacity=25)],
            transform(pos=[CX, CY, 0], opacity=pulse_opacity(op, 20, 40)),
            op=op,
        )
    )

    body = group(
        [
            {
                "ty": "sh",
                "ks": static(
                    {
                        "i": [[0, 0]] * 8,
                        "o": [[0, 0]] * 8,
                        "v": [
                            [0, -90],
                            [70, -70],
                            [85, 20],
                            [55, 75],
                            [0, 85],
                            [-55, 75],
                            [-85, 20],
                            [-70, -70],
                        ],
                        "c": True,
                    }
                ),
                "nm": "Brain Path",
                "hd": False,
            },
            {
                "ty": "fl",
                "c": static(pink + [1]),
                "o": static(100),
                "r": 1,
                "nm": "Fill",
                "hd": False,
            },
            {
                "ty": "st",
                "c": static([c * 0.85 for c in pink] + [1]),
                "o": static(60),
                "w": static(3),
                "lc": 2,
                "lj": 2,
                "nm": "Stroke",
                "hd": False,
            },
            {
                "ty": "tr",
                "p": static([0, 0]),
                "a": static([0, 0]),
                "s": static([100, 100]),
                "r": static(0),
                "o": static(100),
                "sk": static(0),
                "sa": static(0),
                "nm": "Transform",
            },
        ],
        "Brain Body",
    )

    layers.append(
        shape_layer(
            2,
            "Brain",
            [body],
            transform(scale=breathe_scale(op, 5, op), pos=[CX, CY, 0]),
            op=op,
        )
    )

    # Large bright eyes
    for side, ex in [(-1, -28), (1, 28)]:
        layers.append(
            shape_layer(
                10 + side,
                f"Eye {side}",
                [ellipse(18, 22, dark), ellipse(6, 6, [1, 1, 1])],
                transform(pos=[CX + ex, CY + 8, 0]),
                op=op,
                parent=2,
            )
        )

    # Smile
    smile = path(
        [[-22, 28], [0, 42], [22, 28]],
        [[0, 0], [0, 8], [0, 0]],
        [[0, 0], [0, -8], [0, 0]],
        False,
        dark,
        stroke_w=4,
        stroke_color=dark,
    )
    layers.append(
        shape_layer(12, "Smile", [smile], transform(pos=[CX, CY, 0]), op=op, parent=2)
    )

    layers.extend(neural_layers(20, op, pink))
    layers.extend(
        sparkle_layers(30, op, [(-100, -80), (110, -60), (-90, 90), (100, 100), (0, -120)])
    )

    return make_comp("brain_healthy", op, layers)


def build_concerned():
    op = 120
    orange = hex_rgb("FDBA74")
    dark = hex_rgb("1E1E2E")
    layers = []

    layers.append(
        shape_layer(
            1,
            "Glow",
            [ellipse(260, 280, orange, fill_opacity=18)],
            transform(pos=[CX, CY, 0], opacity=pulse_opacity(op, 12, 28)),
            op=op,
        )
    )

    body_shapes = group(
        [
            {
                "ty": "sh",
                "ks": static(
                    {
                        "i": [[0, 0]] * 8,
                        "o": [[0, 0]] * 8,
                        "v": [
                            [0, -88],
                            [68, -68],
                            [82, 18],
                            [52, 72],
                            [0, 82],
                            [-52, 72],
                            [-82, 18],
                            [-68, -68],
                        ],
                        "c": True,
                    }
                ),
                "nm": "Brain",
                "hd": False,
            },
            {"ty": "fl", "c": static(orange + [1]), "o": static(100), "r": 1, "nm": "Fill", "hd": False},
            {
                "ty": "tr",
                "p": static([0, 0]),
                "a": static([0, 0]),
                "s": static([100, 100]),
                "r": static(0),
                "o": static(100),
                "sk": static(0),
                "sa": static(0),
                "nm": "Transform",
            },
        ]
    )

    shake_kf = animated(
        [
            {"t": 0, "s": [0]},
            {"t": 55, "s": [0]},
            {"t": 58, "s": [-6]},
            {"t": 61, "s": [6]},
            {"t": 64, "s": [-4]},
            {"t": 67, "s": [4]},
            {"t": 70, "s": [0]},
            {"t": op, "s": [0]},
        ]
    )

    layers.append(
        shape_layer(
            2,
            "Brain",
            [body_shapes],
            transform(
                scale=breathe_scale(op, 6, op),
                rotation=shake_kf,
                pos=[CX, CY, 0],
            ),
            op=op,
        )
    )

    # Narrowed worried eyes (ovals wider than tall)
    for ex in [-26, 26]:
        layers.append(
            shape_layer(
                5,
                "Eye",
                [ellipse(20, 14, dark), ellipse(5, 4, [1, 1, 1])],
                transform(pos=[CX + ex, CY + 6, 0]),
                op=op,
                parent=2,
            )
        )
        # Brow
        layers.append(
            shape_layer(
                6,
                "Brow",
                [
                    line(ex - 14, -18, ex + 6, -10, dark, 3)
                    if ex < 0
                    else line(ex - 6, -10, ex + 14, -18, dark, 3)
                ],
                transform(pos=[CX, CY, 0]),
                op=op,
                parent=2,
            )
        )

    mouth = path(
        [[-14, 32], [0, 28], [14, 32]],
        [[0, 0], [0, -4], [0, 0]],
        [[0, 0], [0, 4], [0, 0]],
        False,
        dark,
        stroke_w=3,
        stroke_color=dark,
    )
    layers.append(
        shape_layer(8, "Mouth", [mouth], transform(pos=[CX, CY, 0]), op=op, parent=2)
    )

    layers.extend(neural_layers(20, op, orange, flicker=True))
    return make_comp("brain_concerned", op, layers)


def build_tired():
    op = 240
    yellow = hex_rgb("FDE047")
    dark = hex_rgb("1E1E2E")
    layers = []

    layers.append(
        shape_layer(
            1,
            "Glow",
            [ellipse(240, 260, yellow, fill_opacity=12)],
            transform(pos=[CX, CY, 0], opacity=pulse_opacity(op, 8, 18)),
            op=op,
        )
    )

    body_shapes = group(
        [
            {
                "ty": "sh",
                "ks": static(
                    {
                        "i": [[0, 0]] * 8,
                        "o": [[0, 0]] * 8,
                        "v": [
                            [0, -86],
                            [66, -66],
                            [80, 16],
                            [50, 70],
                            [0, 80],
                            [-50, 70],
                            [-80, 16],
                            [-66, -66],
                        ],
                        "c": True,
                    }
                ),
                "nm": "Brain",
                "hd": False,
            },
            {"ty": "fl", "c": static(yellow + [1]), "o": static(100), "r": 1, "nm": "Fill", "hd": False},
            {
                "ty": "tr",
                "p": static([0, 0]),
                "a": static([0, 0]),
                "s": static([100, 100]),
                "r": static(0),
                "o": static(100),
                "sk": static(0),
                "sa": static(0),
                "nm": "Transform",
            },
        ]
    )

    layers.append(
        shape_layer(
            2,
            "Brain",
            [body_shapes],
            transform(pos=sway_position(op, 16), scale=static([98, 98, 100])),
            op=op,
        )
    )

    # Crack on forehead
    crack = path(
        [[-4, -55], [-2, -40], [2, -48], [4, -35]],
        [[0, 0]] * 4,
        [[0, 0]] * 4,
        False,
        [0.3, 0.3, 0.35],
        stroke_w=2,
        stroke_color=[0.3, 0.3, 0.35],
    )
    layers.append(
        shape_layer(3, "Crack", [crack], transform(pos=[CX, CY, 0]), op=op, parent=2)
    )

    # Half-closed eyes (arcs as flat ellipses)
    for ex in [-26, 26]:
        layers.append(
            shape_layer(
                5,
                "Eye",
                [ellipse(22, 8, dark)],
                transform(pos=[CX + ex, CY + 10, 0]),
                op=op,
                parent=2,
            )
        )

    # Tired frown
    frown = path(
        [[-16, 36], [0, 30], [16, 36]],
        [[0, 0], [0, 6], [0, 0]],
        [[0, 0], [0, -6], [0, 0]],
        False,
        dark,
        stroke_w=3,
        stroke_color=dark,
    )
    layers.append(
        shape_layer(8, "Frown", [frown], transform(pos=[CX, CY, 0]), op=op, parent=2)
    )

    layers.extend(neural_layers(20, op, yellow, dim=True))
    return make_comp("brain_tired", op, layers)


def build_melting():
    op = 180
    red = hex_rgb("EF4444")
    dark_red = hex_rgb("991B1B")
    dark = hex_rgb("1E1E2E")
    layers = []

    # Gradient simulated with two overlapping bodies
    layers.append(
        shape_layer(
            1,
            "Glow",
            [ellipse(270, 290, red, fill_opacity=20)],
            transform(pos=[CX, CY, 0], opacity=pulse_opacity(op, 15, 35)),
            op=op,
        )
    )

    outer = group(
        [
            {
                "ty": "sh",
                "ks": static(
                    {
                        "i": [[0, 0]] * 8,
                        "o": [[0, 0]] * 8,
                        "v": [
                            [0, -85],
                            [65, -65],
                            [78, 15],
                            [48, 68],
                            [0, 78],
                            [-48, 68],
                            [-78, 15],
                            [-65, -65],
                        ],
                        "c": True,
                    }
                ),
                "nm": "Brain",
                "hd": False,
            },
            {"ty": "fl", "c": static(red + [1]), "o": static(100), "r": 1, "nm": "Fill", "hd": False},
            {
                "ty": "tr",
                "p": static([0, 0]),
                "a": static([0, 0]),
                "s": static([100, 100]),
                "r": static(0),
                "o": static(100),
                "sk": static(0),
                "sa": static(0),
                "nm": "Transform",
            },
        ]
    )

    inner = ellipse(120, 130, dark_red, fill_opacity=70)

    layers.append(
        shape_layer(
            2,
            "Brain",
            [outer, inner],
            transform(
                scale=animated(
                    [
                        {"t": 0, "s": [100, 100, 100]},
                        {"t": 90, "s": [102, 98, 100]},
                        {"t": op, "s": [100, 100, 100]},
                    ]
                ),
                pos=[CX, CY, 0],
            ),
            op=op,
        )
    )

    # Multiple cracks
    for pts in [
        [[-6, -50], [0, -38], [5, -45]],
        [[-35, -10], [-28, 5], [-20, 15]],
        [[30, 0], [38, 15], [42, 28]],
    ]:
        z = [[0, 0]] * len(pts)
        layers.append(
            shape_layer(
                4,
                "Crack",
                [path(pts, z, z, False, [0.2, 0.1, 0.1], stroke_w=2, stroke_color=[0.2, 0.1, 0.1])],
                transform(pos=[CX, CY, 0]),
                op=op,
                parent=2,
            )
        )

    # Dizzy spiral eyes
    for ex in [-24, 24]:
        spiral = path(
            [[-6, 0], [0, 6], [6, 0], [0, -6], [-6, 0]],
            [[0, 0]] * 5,
            [[0, 0]] * 5,
            False,
            dark,
            stroke_w=3,
            stroke_color=dark,
        )
        layers.append(
            shape_layer(7, "Spiral Eye", [spiral], transform(pos=[CX + ex, CY + 8, 0]), op=op, parent=2)
        )

    # Distressed mouth
    mouth = path(
        [[-12, 34], [0, 26], [12, 34]],
        [[0, 0], [0, -6], [0, 0]],
        [[0, 0], [0, 6], [0, 0]],
        False,
        dark,
        stroke_w=3,
        stroke_color=dark,
    )
    layers.append(
        shape_layer(9, "Mouth", [mouth], transform(pos=[CX, CY, 0]), op=op, parent=2)
    )

    # Dripping blobs
    for i, dx in enumerate([-30, -8, 18, 35]):
        drip_y = animated(
            [
                {"t": 0, "s": [CY + 70, 0, 0]},
                {"t": 60 + i * 15, "s": [CY + 95, 0, 0]},
                {"t": op, "s": [CY + 70, 0, 0]},
            ]
        )
        layers.append(
            shape_layer(
                15 + i,
                f"Drip {i}",
                [ellipse(14, 20, dark_red)],
                transform(pos=animated([{"t": 0, "s": [CX + dx, CY + 70, 0]}, {"t": op, "s": [CX + dx, CY + 70, 0]}])),
                op=op,
            )
        )
        # Fix drip animation
        layers[-1]["ks"]["p"] = animated(
            [
                {"t": 0, "s": [CX + dx, CY + 72, 0]},
                {"t": 45 + i * 10, "s": [CX + dx, CY + 100, 0]},
                {"t": op, "s": [CX + dx, CY + 72, 0]},
            ]
        )

    # Sweat drops
    for i, (sx, sy) in enumerate([(70, -30), (-75, -10), (60, 40)]):
        layers.append(
            shape_layer(
                25 + i,
                f"Sweat {i}",
                [ellipse(10, 14, [0.4, 0.7, 1.0])],
                transform(
                    pos=animated(
                        [
                            {"t": 0, "s": [CX + sx, CY + sy, 0]},
                            {"t": 50 + i * 20, "s": [CX + sx, CY + sy + 40, 0]},
                            {"t": op, "s": [CX + sx, CY + sy, 0]},
                        ]
                    ),
                    opacity=animated(
                        [
                            {"t": 0, "s": [90]},
                            {"t": 50 + i * 20, "s": [0]},
                            {"t": op, "s": [90]},
                        ]
                    ),
                ),
                op=op,
            )
        )

    layers.extend(neural_layers(30, op, red, erratic=True))
    return make_comp("brain_melting", op, layers)


def build_exploding():
    op = 120
    gray = hex_rgb("71717A")
    dark = hex_rgb("1E1E2E")
    layers = []

    layers.append(
        shape_layer(
            2,
            "Brain",
            [
                group(
                    [
                        {
                            "ty": "sh",
                            "ks": static(
                                {
                                    "i": [[0, 0]] * 8,
                                    "o": [[0, 0]] * 8,
                                    "v": [
                                        [0, -82],
                                        [62, -62],
                                        [75, 12],
                                        [45, 65],
                                        [0, 75],
                                        [-45, 65],
                                        [-75, 12],
                                        [-62, -62],
                                    ],
                                    "c": True,
                                }
                            ),
                            "nm": "Brain",
                            "hd": False,
                        },
                        {"ty": "fl", "c": static(gray + [1]), "o": static(100), "r": 1, "nm": "Fill", "hd": False},
                        {
                            "ty": "tr",
                            "p": static([0, 0]),
                            "a": static([0, 0]),
                            "s": static([100, 100]),
                            "r": static(0),
                            "o": static(100),
                            "sk": static(0),
                            "sa": static(0),
                            "nm": "Transform",
                        },
                    ]
                )
            ],
            transform(pos=shake_position(op, 12, 14), rotation=shake_rotation(op, 12, 5)),
            op=op,
        )
    )

    # X eyes
    for ex in [-26, 26]:
        layers.append(
            shape_layer(
                5,
                "X Eye",
                [
                    line(-8, -8, 8, 8, dark, 4),
                    line(-8, 8, 8, -8, dark, 4),
                ],
                transform(pos=[CX + ex, CY + 8, 0]),
                op=op,
                parent=2,
            )
        )

    # Open overwhelmed mouth
    layers.append(
        shape_layer(
            8,
            "Mouth",
            [ellipse(24, 18, dark)],
            transform(pos=[CX, CY + 38, 0]),
            op=op,
            parent=2,
        )
    )

    # Floating brain pieces
    pieces = [(-90, -60), (95, -40), (-70, 80), (85, 70), (0, -110)]
    for i, (px, py) in enumerate(pieces):
        layers.append(
            shape_layer(
                15 + i,
                f"Piece {i}",
                [ellipse(22, 18, gray)],
                transform(
                    pos=animated(
                        [
                            {"t": 0, "s": [CX + px * 0.5, CY + py * 0.5, 0]},
                            {"t": op // 2, "s": [CX + px, CY + py, 0]},
                            {"t": op, "s": [CX + px * 0.5, CY + py * 0.5, 0]},
                        ]
                    ),
                    rotation=animated(
                        [
                            {"t": 0, "s": [0]},
                            {"t": op, "s": [180]},
                        ]
                    ),
                    opacity=animated(
                        [
                            {"t": 0, "s": [100]},
                            {"t": op // 2, "s": [60]},
                            {"t": op, "s": [100]},
                        ]
                    ),
                ),
                op=op,
            )
        )

    # Smoke particles
    for i in range(6):
        angle = i * 60
        ox = math.cos(math.radians(angle)) * 90
        oy = math.sin(math.radians(angle)) * 90
        layers.append(
            shape_layer(
                25 + i,
                f"Smoke {i}",
                [ellipse(28, 28, [0.5, 0.5, 0.55], fill_opacity=40)],
                transform(
                    pos=animated(
                        [
                            {"t": 0, "s": [CX + ox * 0.3, CY + oy * 0.3, 0]},
                            {"t": op, "s": [CX + ox, CY + oy - 30, 0]},
                        ]
                    ),
                    opacity=animated(
                        [
                            {"t": 0, "s": [50]},
                            {"t": op, "s": [0]},
                        ]
                    ),
                    scale=animated(
                        [
                            {"t": 0, "s": [60, 60, 100]},
                            {"t": op, "s": [120, 120, 100]},
                        ]
                    ),
                ),
                op=op,
            )
        )

    # Broken sparking neural lines
    for i, (x1, y1, x2, y2) in enumerate([(-35, -15, 25, -25), (20, -20, 45, 5), (-40, 20, 0, 35)]):
        layers.append(
            shape_layer(
                35 + i,
                f"Spark {i}",
                [line(x1, y1, x2, y2, [1, 0.8, 0.2], 3, animated([{"t": 0, "s": [100]}, {"t": 6, "s": [0]}, {"t": 12, "s": [100]}, {"t": op, "s": [100]}]))],
                transform(pos=[CX, CY, 0]),
                op=op,
            )
        )

    return make_comp("brain_exploding", op, layers)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    files = {
        "brain_healthy": build_healthy(),
        "brain_concerned": build_concerned(),
        "brain_tired": build_tired(),
        "brain_melting": build_melting(),
        "brain_exploding": build_exploding(),
    }
    for name, data in files.items():
        path = OUT / f"{name}.json"
        with path.open("w", encoding="utf-8") as f:
            json.dump(data, f, separators=(",", ":"))
        print(f"Wrote {path} ({path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
