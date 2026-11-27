package com.fws.playmind.core.bench

/**
 * Điểm vào của bộ đo bot (chạy trong CI bởi .github/workflows/bot-bench.yml, hoặc tại máy):
 *   gauge  chess <stockfish> <elo> <games> <ourMs> <sfMs> <seed>      đấu Stockfish giới hạn sức (BOT_GUIDE mục 3.5)
 *   ladder <game> <A> <B> <pairs> <svMs> <seed> [players]             đấu hai cấp của cùng game (mục 3.4)
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "gauge" -> gaugeMain(args.drop(1))
        "ladder" -> ladderMain(args.drop(1))
        else -> error("usage: gauge|ladder ...")
    }
}
