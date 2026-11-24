"""Sinh app/.../ui/ChessArt.kt từ chess_pieces.py:  python3 gen_kotlin.py"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from dsl import kotlin_layers
import chess_pieces as cp

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'app/src/main/kotlin/com/fws/playmind/ui/ChessArt.kt')

def kcol(h): return f"Color(0xFF{h.lstrip('#').upper()})"

src = '''package com.fws.playmind.ui

// ───────────────────────────────────────────────────────────────────────────────────────────
// FILE ĐƯỢC SINH TỰ ĐỘNG bởi tools/art/gen_kotlin.py từ tools/art/chess_pieces.py - đừng sửa tay.
// Muốn đổi hình quân cờ: sửa chess_pieces.py, chạy preview.py để xem, rồi chạy lại gen_kotlin.py.
// ───────────────────────────────────────────────────────────────────────────────────────────

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.PathParser

internal sealed interface Layer
internal class LFill(val path: Path, val brush: Brush) : Layer
internal class LStroke(val path: Path, val color: Color, val width: Float) : Layer
internal class LClip(val path: Path, val layers: List<Layer>) : Layer

/** Vẽ danh sách lớp trong hệ tọa độ 100x100 hiện hành của DrawScope. */
internal fun DrawScope.drawLayers(layers: List<Layer>) {
    for (l in layers) when (l) {
        is LFill -> drawPath(l.path, l.brush)
        is LStroke -> drawPath(l.path, l.color, style = Stroke(width = l.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        is LClip -> clipPath(l.path) { drawLayers(l.layers) }
    }
}

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

/** Bảng màu thân quân (bên trắng = bạc ngọc trai, bên đen = đen huyền). Vàng là màu cố định. */
internal class BodyPal(val bEdge: Color, val bShade: Color, val bMid: Color, val bHi: Color, val ink: Color)

internal object ChessArt {
    val white = BodyPal(%(w)s)
    val black = BodyPal(%(b)s)

    private val cache = HashMap<Int, List<Layer>>()

    /** [type] theo ChessGame (1 tốt, 2 mã, 3 tượng, 4 xe, 5 hậu, 6 vua). */
    fun layers(type: Int, isWhite: Boolean): List<Layer> = cache.getOrPut(type * 2 + (if (isWhite) 1 else 0)) {
        val p = if (isWhite) white else black
        when (type) {
%(whens)s
            else -> emptyList()
        }
    }

%(funs)s
}
'''
def pal(P): return ', '.join(kcol(P[k]) for k in ['bEdge','bShade','bMid','bHi','ink'])
names = {1:'pawn',2:'knight',3:'bishop',4:'rook',5:'queen',6:'king'}
funs=''; whens=''
for t,(_,fn) in cp.PIECES.items():
    funs += f"    private fun {names[t]}(p: BodyPal): List<Layer> = listOf(\n{kotlin_layers(fn(), 8)}\n    )\n\n"
    whens += f"            {t} -> {names[t]}(p)\n"
out = src % dict(w=pal(cp.PAL_WHITE), b=pal(cp.PAL_BLACK), funs=funs.rstrip()+'\n', whens=whens.rstrip())
open(OUT,'w',encoding='utf-8').write(out)
print('ghi', os.path.normpath(OUT), len(out)//1024, 'KB')


# ───────────── OtherArt.kt: cờ tướng (kính) + caro (gỗ) ─────────────
import other_pieces as op
OUT2 = os.path.join(os.path.dirname(__file__), '..', '..', 'app/src/main/kotlin/com/fws/playmind/ui/OtherArt.kt')
hdr = '''package com.fws.playmind.ui

// FILE ĐƯỢC SINH TỰ ĐỘNG bởi tools/art/gen_kotlin.py từ tools/art/other_pieces.py - đừng sửa tay.

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathParser

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

/** Đĩa kính cờ tướng (hệ tọa độ 100x100, tâm 50,50, bán kính 46). Chữ khắc được vẽ riêng. */
internal object XiangqiArt {
    val red: List<Layer> by lazy { listOf(
%s
    ) }
    val black: List<Layer> by lazy { listOf(
%s
    ) }
}

/** Quân caro bằng gỗ (hệ tọa độ 100x100): X gỗ sẫm, O vòng gỗ sáng. */
internal object CaroArt {
    val x: List<Layer> by lazy { listOf(
%s
    ) }
    val o: List<Layer> by lazy { listOf(
%s
    ) }
}
'''
open(OUT2, 'w', encoding='utf-8').write(hdr % tuple(kotlin_layers(l, 8) for l in (op.XQ_RED, op.XQ_BLACK, op.caro_x(), op.caro_o())))
print('ghi OtherArt.kt')
