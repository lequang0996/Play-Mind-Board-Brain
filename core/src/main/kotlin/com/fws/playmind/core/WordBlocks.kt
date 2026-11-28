package com.fws.playmind.core

import kotlin.random.Random

/**
 * Khối gỗ ghép chữ: một chủ đề (quốc gia, con vật, ...) và các khối gỗ chữ A-Z xáo trộn, đúng bằng chữ cái của 7 từ
 * tiếng Anh thuộc chủ đề. Người chơi có 10 phút để ghép đủ 7 từ, mỗi khối chỉ dùng một lần.
 * Mỗi đề có đúng MỘT lời giải: trong chủ đề chỉ có duy nhất một bộ 7 từ dùng vừa hết các khối, và game chỉ nhận 7 từ đó.
 */
object WordBlocks {
    const val WORDS = 7
    const val SECONDS = 600
    const val MIN_LEN = 3
    const val MAX_LEN = 8

    class Theme(val id: String, words: String) {
        val words: Set<String> = words.trim().split(Regex("\\s+")).toSet()
    }

    private fun t(id: String, w: String) = Theme(id, w)

    /** Các chủ đề. Tên chủ đề dịch theo khóa chuỗi `wb_theme_<id>`; từ luôn là tiếng Anh, chỉ chữ A-Z, 3-8 chữ cái. */
    val THEMES: List<Theme> = listOf(
        t("countries", """
            QATAR JAPAN INDIA BRAZIL SPAIN CANADA FINLAND CHINA FRANCE ITALY GERMANY EGYPT KENYA MEXICO PERU CHILE CUBA IRAN IRAQ
            OMAN NEPAL GREECE TURKEY RUSSIA SWEDEN NORWAY POLAND AUSTRIA BELGIUM DENMARK IRELAND ICELAND PORTUGAL ISRAEL JORDAN
            KUWAIT LEBANON SYRIA YEMEN MALI CHAD NIGER NIGERIA GHANA SUDAN LIBYA TUNISIA MOROCCO ANGOLA ZAMBIA UGANDA RWANDA SOMALIA
            LAOS THAILAND VIETNAM KOREA TAIWAN BHUTAN MALTA CYPRUS SERBIA CROATIA ALBANIA LATVIA ROMANIA HUNGARY UKRAINE ECUADOR
            BOLIVIA URUGUAY PANAMA HAITI JAMAICA FIJI TONGA SAMOA TOGO GABON BENIN GUINEA MONGOLIA PAKISTAN ESTONIA SLOVAKIA SLOVENIA
            BULGARIA BAHRAIN ARMENIA GEORGIA LIBERIA MALAWI NAMIBIA ERITREA BURUNDI"""),
        t("animals", """
            LION TIGER BEAR WOLF FOX ZEBRA HORSE MONKEY RABBIT DONKEY CAMEL GIRAFFE PANDA KOALA SHEEP GOAT COW PIG DOG CAT MOUSE
            SNAKE EAGLE OWL DEER MOOSE OTTER BEAVER HIPPO RHINO LEOPARD CHEETAH JAGUAR GORILLA BADGER HAMSTER TURTLE LIZARD FROG
            TOAD SHARK WHALE DOLPHIN PENGUIN PARROT RAVEN CROW DUCK GOOSE SWAN CHICKEN BAT SQUIRREL KANGAROO LLAMA BISON BUFFALO
            SEAL WALRUS CRAB OCTOPUS SPIDER ANT BEE WASP MOLE HEDGEHOG ELEPHANT"""),
        t("fruits", """
            APPLE BANANA MANGO ORANGE LEMON LIME GRAPE PEAR PEACH PLUM CHERRY MELON KIWI PAPAYA GUAVA LYCHEE DURIAN COCONUT
            APRICOT FIG DATE OLIVE PRUNE QUINCE BERRY RAISIN AVOCADO LONGAN POMELO RAMBUTAN MULBERRY KUMQUAT"""),
        t("colors", """
            RED BLUE GREEN YELLOW ORANGE PURPLE PINK BROWN BLACK WHITE GREY GRAY SILVER GOLD BEIGE CYAN MAGENTA VIOLET INDIGO
            MAROON NAVY OLIVE TEAL CORAL IVORY LILAC AMBER CRIMSON SCARLET KHAKI TAN LIME PEACH MINT LAVENDER EMERALD BRONZE CREAM RUBY"""),
        t("sports", """
            TENNIS GOLF SOCCER RUGBY BOXING JUDO KARATE CHESS SKIING SURFING ROWING FENCING HOCKEY CRICKET BASEBALL CYCLING
            DIVING ARCHERY SAILING POLO SQUASH BOWLING DARTS SNOOKER KAYAKING SKATING CLIMBING RUNNING SWIMMING HANDBALL SOFTBALL
            NETBALL LACROSSE MARATHON HURDLES JAVELIN"""),
        t("jobs", """
            DOCTOR NURSE TEACHER LAWYER PILOT CHEF BAKER FARMER DRIVER SINGER ACTOR DANCER WRITER POET JUDGE OFFICER SOLDIER
            SAILOR BARBER BUTCHER DENTIST PAINTER PLUMBER TAILOR WAITER CASHIER BANKER MINER CLERK GUARD COACH ENGINEER ARTIST
            AUTHOR EDITOR VET SURGEON FIREMAN MECHANIC GARDENER MUSICIAN JOCKEY"""),
        t("body", """
            HEAD HAIR FACE EYE EAR NOSE MOUTH LIP TOOTH TONGUE CHIN CHEEK NECK SHOULDER ARM ELBOW WRIST HAND FINGER THUMB NAIL
            CHEST BACK WAIST HIP LEG KNEE ANKLE FOOT TOE HEEL HEART LUNG LIVER KIDNEY BRAIN SKIN BONE BLOOD STOMACH MUSCLE SPINE
            SKULL THROAT EYEBROW EYELASH FOREHEAD BELLY PALM"""),
        t("vegetables", """
            CARROT POTATO TOMATO ONION GARLIC CABBAGE LETTUCE SPINACH PEPPER CUCUMBER PUMPKIN CELERY RADISH TURNIP PEA BEAN CORN
            LEEK BEET KALE OKRA GINGER PARSLEY SQUASH YAM BROCCOLI ZUCCHINI EGGPLANT SHALLOT CHILI MUSHROOM LENTIL"""),
        t("instruments", """
            PIANO GUITAR VIOLIN DRUM FLUTE HARP CELLO TRUMPET BANJO ORGAN TUBA OBOE CLARINET TROMBONE UKULELE BAGPIPE GONG BELL
            TRIANGLE HORN LUTE SITAR LYRE VIOLA BUGLE PICCOLO MANDOLIN CYMBAL BASSOON MARIMBA KAZOO"""),
        t("kitchen", """
            SPOON FORK KNIFE PLATE BOWL CUP MUG GLASS PAN POT KETTLE OVEN STOVE TOASTER BLENDER FRIDGE SINK LADLE WHISK GRATER
            TONGS APRON TRAY JAR TEAPOT SPATULA COLANDER NAPKIN SAUCER PITCHER SKILLET STRAINER BOTTLE TIMER MIXER FREEZER"""),
        t("weather", """
            RAIN SNOW WIND STORM CLOUD SUN FOG MIST HAIL THUNDER RAINBOW BREEZE FROST ICE TORNADO TYPHOON DROUGHT FLOOD HEAT DEW
            SLEET DRIZZLE BLIZZARD MONSOON SUNSHINE CYCLONE GALE GUST SHOWER"""),
        t("clothes", """
            SHIRT DRESS SKIRT JACKET COAT SCARF GLOVES HAT CAP SOCKS SHOES BOOTS JEANS SHORTS SWEATER HOODIE BLOUSE SUIT TIE BELT
            VEST PAJAMAS SANDALS SLIPPERS TROUSERS UNIFORM KIMONO SARI APRON MITTENS TUXEDO BIKINI PONCHO CARDIGAN BLAZER LEGGINGS"""),
        t("transport", """
            CAR BUS TRAIN TRUCK BIKE TAXI PLANE SHIP BOAT TRAM SUBWAY SCOOTER TRACTOR VAN JEEP YACHT CANOE KAYAK FERRY ROCKET
            BICYCLE LORRY CARAVAN GLIDER AIRSHIP BALLOON SLED SLEDGE CART WAGON CAB RAFT BARGE"""),
        t("space", """
            SUN MOON STAR MARS VENUS EARTH SATURN JUPITER URANUS NEPTUNE MERCURY PLUTO COMET GALAXY NEBULA ORBIT ROCKET METEOR
            ASTEROID ECLIPSE COSMOS QUASAR PULSAR CRATER UNIVERSE PLANET GRAVITY"""),
        t("cities", """
            PARIS LONDON TOKYO ROME MADRID BERLIN MOSCOW CAIRO DELHI BEIJING SEOUL HANOI BANGKOK DUBAI LIMA OSLO VIENNA PRAGUE
            ATHENS DUBLIN LISBON WARSAW SYDNEY TORONTO BOSTON CHICAGO MIAMI DALLAS DENVER SEATTLE NAIROBI LAGOS MANILA JAKARTA
            HAVANA QUITO BOGOTA SANTIAGO CARACAS OTTAWA HELSINKI BRUSSELS ZURICH GENEVA MILAN VENICE NAPLES MUNICH HAMBURG KYOTO
            OSAKA SHANGHAI TAIPEI"""),
    )

    fun theme(id: String): Theme = THEMES.first { it.id == id }

    /**
     * Sinh một đề: chọn chủ đề ngẫu nhiên, 7 từ khác nhau (ưu tiên 4-7 chữ cái, tổng 30-42 chữ cái như ví dụ quốc gia),
     * rồi xáo trộn toàn bộ chữ cái thành các khối gỗ. Chỉ nhận bộ 7 từ là cách DUY NHẤT trong chủ đề dùng vừa hết các khối
     * ([countSolutions] == 1); không thì bốc lại.
     */
    fun generate(rnd: Random, themeId: String? = null): WordBlocksGame {
        var fallback: WordBlocksGame? = null
        for (attempt in 0 until 400) {
            val th = if (themeId != null) theme(themeId) else THEMES[rnd.nextInt(THEMES.size)]
            val pool = th.words.filter { it.length in 4..7 }.ifEmpty { th.words.toList() }
            val pick = pool.shuffled(rnd).take(WORDS)
            if (pick.sumOf { it.length } !in 30..42) continue
            val letters = pick.joinToString("").toCharArray()
            letters.shuffle(rnd)
            val g = WordBlocksGame(th, pick, letters)
            if (countSolutions(th, letters, 2) == 1) return g
            if (fallback == null) fallback = g
        }
        return fallback ?: generate(rnd, themeId)
    }

    /**
     * Đếm số bộ [WORDS] từ khác nhau của chủ đề dùng vừa hết đúng các chữ cái [letters] (dừng khi đạt [limit]).
     * Chỉ xét các từ ghép được từ [letters]; duyệt tổ hợp theo chỉ số tăng dần, cắt nhánh khi thiếu chữ.
     */
    fun countSolutions(th: Theme, letters: CharArray, limit: Int = 2): Int {
        val have = IntArray(26); for (c in letters) have[c - 'A']++
        val cand = th.words.map { w -> IntArray(26).also { for (c in w) it[c - 'A']++ } to w.length }
            .filter { (cnt, _) -> (0 until 26).all { cnt[it] <= have[it] } }
            .sortedByDescending { it.second }
        var found = 0
        val left = have.copyOf()
        fun rec(start: Int, picked: Int, remaining: Int) {
            if (found >= limit) return
            if (picked == WORDS) { if (remaining == 0) found++; return }
            val need = WORDS - picked
            for (i in start until cand.size) {
                if (cand.size - i < need) return
                val (cnt, len) = cand[i]
                // từ dài nhất còn lại không đủ lấp số chữ còn thiếu thì dừng (cand xếp dài -> ngắn)
                if (len * need < remaining) return
                if (len > remaining) continue
                var ok = true
                for (k in 0 until 26) if (cnt[k] > left[k]) { ok = false; break }
                if (!ok) continue
                for (k in 0 until 26) left[k] -= cnt[k]
                rec(i + 1, picked + 1, remaining - len)
                for (k in 0 until 26) left[k] += cnt[k]
                if (found >= limit) return
            }
        }
        rec(0, 0, letters.size)
        return found
    }
}

/** Trạng thái một ván Khối gỗ ghép chữ. */
class WordBlocksGame(val theme: WordBlocks.Theme, val answers: List<String>, val blocks: CharArray) {
    enum class Submit { OK, TOO_SHORT, NOT_IN_THEME, NOT_ANSWER, DUPLICATE, EMPTY }

    /** Khối đang nằm trong một từ đã ghép hoặc trong từ đang ghép. */
    val used = BooleanArray(blocks.size)
    /** Chỉ số khối của từ đang ghép, theo thứ tự chạm. */
    val current = ArrayList<Int>()
    /** Các từ đã ghép: từ và chỉ số khối đã dùng. */
    val found = ArrayList<Pair<String, IntArray>>()

    val word: String get() = current.joinToString("") { blocks[it].toString() }
    val solved: Boolean get() = found.size >= WordBlocks.WORDS

    /** Chạm một khối: khối rảnh thì thêm vào từ đang ghép, khối đang ở trong từ đang ghép thì gỡ ra. */
    fun tap(i: Int): Boolean {
        if (i !in blocks.indices || solved) return false
        val k = current.indexOf(i)
        if (k >= 0) { current.removeAt(k); used[i] = false; return true }
        if (used[i] || current.size >= WordBlocks.MAX_LEN) return false
        current.add(i); used[i] = true
        return true
    }

    fun back() { if (current.isNotEmpty()) used[current.removeAt(current.size - 1)] = false }
    fun clear() { for (i in current) used[i] = false; current.clear() }

    fun submit(): Submit {
        if (current.isEmpty()) return Submit.EMPTY
        val w = word
        if (w.length < WordBlocks.MIN_LEN) return Submit.TOO_SHORT
        if (found.any { it.first == w }) return Submit.DUPLICATE
        if (w !in theme.words) return Submit.NOT_IN_THEME
        // đề chỉ có một lời giải: từ đúng chủ đề nhưng không thuộc 7 đáp án sẽ làm các khối còn lại không ghép hết được
        if (w !in answers) return Submit.NOT_ANSWER
        found.add(w to current.toIntArray()); current.clear()
        return Submit.OK
    }

    /** Trả từ đã ghép thứ [k] về lại các khối. */
    fun unfind(k: Int): Boolean {
        if (k !in found.indices || solved) return false
        for (i in found.removeAt(k).second) used[i] = false
        return true
    }
}
