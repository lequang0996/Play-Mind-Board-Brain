package com.fws.playmind.ui

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
        LFill(path("M4 50A46 46 0 1 1 96 50A46 46 0 1 1 4 50Z"), Brush.radialGradient(0f to Color(0x80FFE3E6), 0.55f to Color(0x99F58C97), 0.86f to Color(0xCCD8334A), 1f to Color(0xF297132B), center = Offset(40f, 38f), radius = 70f)),
        LStroke(path("M6.87 42.39A43.8 43.8 0 0 1 71.9 12.07"), Color(0xD9FFFFFF), 2.6f),
        LStroke(path("M91.16 64.98A43.8 43.8 0 0 1 35.02 91.16"), Color(0x596A0A1C), 2.6f),
        LStroke(path("M4.6 50A45.4 45.4 0 1 1 95.4 50A45.4 45.4 0 1 1 4.6 50Z"), Color(0x8C6A0A1C), 1.2f),
        LStroke(path("M13 50A37 37 0 1 1 87 50A37 37 0 1 1 13 50Z"), Color(0x66FFFFFF), 1.4f),
        LStroke(path("M83.45 62.18A35.6 35.6 0 0 1 37.82 83.45"), Color(0x406A0A1C), 1.2f),
        LStroke(path("M16.8 50A33.2 33.2 0 1 1 83.2 50A33.2 33.2 0 1 1 16.8 50Z"), Color(0xD9B3122B), 2.0f),
        LFill(path("M18 27A22 9 0 1 0 62 27A22 9 0 1 0 18 27Z"), Brush.linearGradient(0f to Color(0xA6FFFFFF), 1f to Color(0x00FFFFFF), start = Offset(0f, 18f), end = Offset(0f, 36f))),
        LFill(path("M48 75A15 5 0 1 0 78 75A15 5 0 1 0 48 75Z"), Brush.linearGradient(0f to Color(0x00FFFFFF), 1f to Color(0x40FFFFFF), start = Offset(0f, 70f), end = Offset(0f, 80f))),
    ) }
    val black: List<Layer> by lazy { listOf(
        LFill(path("M4 50A46 46 0 1 1 96 50A46 46 0 1 1 4 50Z"), Brush.radialGradient(0f to Color(0x4CFFFFFF), 0.55f to Color(0x808A8A99), 0.86f to Color(0xD125252F), 1f to Color(0xF707070B), center = Offset(40f, 38f), radius = 70f)),
        LStroke(path("M6.87 42.39A43.8 43.8 0 0 1 71.9 12.07"), Color(0xD9FFFFFF), 2.6f),
        LStroke(path("M91.16 64.98A43.8 43.8 0 0 1 35.02 91.16"), Color(0x33000000), 2.6f),
        LStroke(path("M4.6 50A45.4 45.4 0 1 1 95.4 50A45.4 45.4 0 1 1 4.6 50Z"), Color(0x47000000), 1.2f),
        LStroke(path("M13 50A37 37 0 1 1 87 50A37 37 0 1 1 13 50Z"), Color(0x66FFFFFF), 1.4f),
        LStroke(path("M83.45 62.18A35.6 35.6 0 0 1 37.82 83.45"), Color(0x2E000000), 1.2f),
        LStroke(path("M16.8 50A33.2 33.2 0 1 1 83.2 50A33.2 33.2 0 1 1 16.8 50Z"), Color(0xD90C0C12), 2.0f),
        LFill(path("M18 27A22 9 0 1 0 62 27A22 9 0 1 0 18 27Z"), Brush.linearGradient(0f to Color(0xA6FFFFFF), 1f to Color(0x00FFFFFF), start = Offset(0f, 18f), end = Offset(0f, 36f))),
        LFill(path("M48 75A15 5 0 1 0 78 75A15 5 0 1 0 48 75Z"), Brush.linearGradient(0f to Color(0x00FFFFFF), 1f to Color(0x40FFFFFF), start = Offset(0f, 70f), end = Offset(0f, 80f))),
    ) }
}

/** Quân caro bằng gỗ (hệ tọa độ 100x100): X gỗ sẫm, O vòng gỗ sáng. */
internal object CaroArt {
    val x: List<Layer> by lazy { listOf(
        LFill(path("M30.6 21.68Q32.86 19.41 35.12 21.68L82.92 69.48Q85.19 71.74 82.92 74L72.6 84.32Q70.34 86.59 68.08 84.32L20.28 36.52Q18.01 34.26 20.28 32Z"), SolidColor(Color(0xFF150C0B))),
        LFill(path("M20.28 74Q18.01 71.74 20.28 69.48L68.08 21.68Q70.34 19.41 72.6 21.68L82.92 32Q85.19 34.26 82.92 36.52L35.12 84.32Q32.86 86.59 30.6 84.32Z"), SolidColor(Color(0xFF150C0B))),
        LFill(path("M29 18.68Q31.26 16.41 33.52 18.68L81.32 66.48Q83.59 68.74 81.32 71L71 81.32Q68.74 83.59 66.48 81.32L18.68 33.52Q16.41 31.26 18.68 29Z"), Brush.linearGradient(0f to Color(0xFF6A524C), 0.45f to Color(0xFF473430), 1f to Color(0xFF2B1E1D), start = Offset(18f, 18f), end = Offset(82f, 82f))),
        LFill(path("M18.68 71Q16.41 68.74 18.68 66.48L66.48 18.68Q68.74 16.41 71 18.68L81.32 29Q83.59 31.26 81.32 33.52L33.52 81.32Q31.26 83.59 29 81.32Z"), Brush.linearGradient(0f to Color(0xFF6A524C), 0.45f to Color(0xFF473430), 1f to Color(0xFF2B1E1D), start = Offset(18f, 18f), end = Offset(82f, 82f))),
        LClip(path("M29 18.68Q31.26 16.41 33.52 18.68L81.32 66.48Q83.59 68.74 81.32 71L71 81.32Q68.74 83.59 66.48 81.32L18.68 33.52Q16.41 31.26 18.68 29Z"), listOf(
            LStroke(path("M26.31 17.12L82.88 73.69"), Color(0x12FFFFFF), 0.8f),
            LStroke(path("M23.27 20.16L79.84 76.73"), Color(0x12FFFFFF), 0.8f),
            LStroke(path("M20.02 23.41L76.59 79.98"), Color(0x12FFFFFF), 0.8f),
            LStroke(path("M16.91 26.52L73.48 83.09"), Color(0x12FFFFFF), 0.8f),
        )),
        LClip(path("M18.68 71Q16.41 68.74 18.68 66.48L66.48 18.68Q68.74 16.41 71 18.68L81.32 29Q83.59 31.26 81.32 33.52L33.52 81.32Q31.26 83.59 29 81.32Z"), listOf(
            LStroke(path("M17.12 73.69L73.69 17.12"), Color(0x12FFFFFF), 0.8f),
            LStroke(path("M20.16 76.73L76.73 20.16"), Color(0x12FFFFFF), 0.8f),
            LStroke(path("M23.41 79.98L79.98 23.41"), Color(0x12FFFFFF), 0.8f),
            LStroke(path("M26.52 83.09L83.09 26.52"), Color(0x12FFFFFF), 0.8f),
        )),
        LStroke(path("M29 18.68Q31.26 16.41 33.52 18.68L81.32 66.48Q83.59 68.74 81.32 71L71 81.32Q68.74 83.59 66.48 81.32L18.68 33.52Q16.41 31.26 18.68 29Z"), Color(0x1AFFFFFF), 0.9f),
        LStroke(path("M18.68 71Q16.41 68.74 18.68 66.48L66.48 18.68Q68.74 16.41 71 18.68L81.32 29Q83.59 31.26 81.32 33.52L33.52 81.32Q31.26 83.59 29 81.32Z"), Color(0x1AFFFFFF), 0.9f),
        LStroke(path("M21 31L31 21"), Color(0x2EFFFFFF), 1.0f),
    ) }
    val o: List<Layer> by lazy { listOf(
        LFill(path("M17.4 53.2A34 34 0 1 1 85.4 53.2A34 34 0 1 1 17.4 53.2ZM31.4 53.2A20 20 0 1 0 71.4 53.2A20 20 0 1 0 31.4 53.2Z"), SolidColor(Color(0xFFA98C5C))),
        LFill(path("M16 50A34 34 0 1 1 84 50A34 34 0 1 1 16 50ZM30 50A20 20 0 1 0 70 50A20 20 0 1 0 30 50Z"), Brush.linearGradient(0f to Color(0xFFFFFBF0), 0.5f to Color(0xFFF1E3C6), 1f to Color(0xFFDCC699), start = Offset(18f, 18f), end = Offset(82f, 82f))),
        LStroke(path("M16.4 50A33.6 33.6 0 1 1 83.6 50A33.6 33.6 0 1 1 16.4 50Z"), Color(0x8C8A6B3C), 1.1f),
        LStroke(path("M29.6 50A20.4 20.4 0 1 1 70.4 50A20.4 20.4 0 1 1 29.6 50Z"), Color(0xA68A6B3C), 1.2f),
        LStroke(path("M23.44 42.88A27.5 27.5 0 0 1 57.12 23.44"), Color(0xB2FFFFFF), 1.4f),
        LClip(path("M16 50A34 34 0 1 1 84 50A34 34 0 1 1 16 50ZM30 50A20 20 0 1 0 70 50A20 20 0 1 0 30 50Z"), listOf(
            LStroke(path("M18 28Q50 31 82 27"), Color(0x218A6B3C), 0.8f),
            LStroke(path("M18 38Q50 41 82 37"), Color(0x218A6B3C), 0.8f),
            LStroke(path("M18 47Q50 50 82 46"), Color(0x218A6B3C), 0.8f),
            LStroke(path("M18 56Q50 59 82 55"), Color(0x218A6B3C), 0.8f),
            LStroke(path("M18 66Q50 69 82 65"), Color(0x218A6B3C), 0.8f),
            LStroke(path("M18 74Q50 77 82 73"), Color(0x218A6B3C), 0.8f),
        )),
    ) }
}
