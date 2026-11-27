package com.fws.playmind.core.bench

/**
 * Điểm vào của bộ đo bot (chạy trong CI bởi .github/workflows/bot-bench.yml, hoặc tại máy):
 *   [-Dkhóa=giá trị ...] gauge  chess|xiangqi <engine> <elo> <games> <ourMs> <engMs> <seed> [threads] [evalFile]   đấu Stockfish / Pikafish (mục 3.5)
 *   [-Dkhóa=giá trị ...] ladder <game> <A> <B> <pairs> <svMs> <seed> [players]                đấu hai cấp (mục 3.4)
 *   [-Dkhóa=giá trị ...] ab     <game> <A> <B> <games> <ms> <seed> [nodes]                     so hai biến thể engine (mục 2.6)
 *   texel datagen|tune ...   tinh chỉnh Texel (Texel.kt)
 * Các đối số "-Dkhóa=giá trị" ở đầu được đặt thành thuộc tính hệ thống trước khi chạy, ví dụ
 * -Dpm.level.chess.5=n15000 (ghi đè cấu hình cấp, xem BotLevels.override) hoặc -Dpm.chess.threads=2.
 */
fun main(argv: Array<String>) {
    val args = argv.toMutableList()
    while (args.isNotEmpty() && args[0].startsWith("-D")) {
        val kv = args.removeAt(0).removePrefix("-D"); val i = kv.indexOf('=')
        System.setProperty(kv.substring(0, i), kv.substring(i + 1))
    }
    when (args.firstOrNull()) {
        "gauge" -> gaugeMain(args.drop(1))
        "ladder" -> ladderMain(args.drop(1))
        "ab" -> abMain(args.drop(1))
        "texel" -> texelMain(args.drop(1))
        else -> error("usage: [-Dk=v ...] gauge|ladder|ab|texel ...")
    }
}
