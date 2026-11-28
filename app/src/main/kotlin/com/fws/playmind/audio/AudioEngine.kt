package com.fws.playmind.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Hiệu ứng âm thanh của app. */
enum class Sfx { MOVE, CHECK, WIN, LOSE, DRAW, REWARD, TAP }

/**
 * Âm thanh của app, **tổng hợp hoàn toàn bằng mã** (không có tệp nhạc nào, không lo bản quyền, không tăng dung lượng APK).
 *
 * * Nhạc nền: ambient sinh ngẫu nhiên, không bao giờ lặp y hệt. Pad hợp âm 7 / 9 vào ra chậm, bass trầm, vài nốt
 *   kalimba thưa trên âm giai ngũ cung, delay stereo cho không gian. Nhịp 60, âm lượng thấp, không có giai điệu nổi bật,
 *   để không làm phân tâm khi suy nghĩ. Trong ván ([calm] = true) nhạc thưa và dịu hơn ở menu.
 * * Hiệu ứng: tiếng gõ quân gỗ, chuông chiếu tướng, hợp âm thắng / thua / hòa, tiếng lấp lánh khi nhận thưởng.
 *
 * Một luồng duy nhất trộn nhạc và hiệu ứng rồi ghi vào một [AudioTrack]. Luồng chỉ chạy khi app ở tiền cảnh và có thứ
 * để phát; tắt nhạc thì luồng dừng sau khi hiệu ứng cuối cùng phát xong. Nếu app khác đang phát nhạc thì không bật nhạc nền.
 */
class AudioEngine(ctx: Context) {
    private val sp = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var musicOn by mutableStateOf(sp.getBoolean("music_on", true)); private set
    var sfxOn by mutableStateOf(sp.getBoolean("sfx_on", true)); private set

    /** true khi đang trong ván: nhạc thưa và dịu hơn. */
    @Volatile var calm = false

    @Volatile private var foreground = false
    @Volatile private var musicWanted = false
    private val pending = ConcurrentLinkedQueue<FloatArray>()
    private val bank: Map<Sfx, FloatArray> by lazy { SfxBank.render() }
    private var thread: Thread? = null
    private val lock = Any()

    fun updateMusic(v: Boolean) { musicOn = v; sp.edit().putBoolean("music_on", v).apply(); refresh() }
    fun updateSfx(v: Boolean) { sfxOn = v; sp.edit().putBoolean("sfx_on", v).apply() }

    fun play(s: Sfx) {
        if (!sfxOn || !foreground) return
        pending.add(bank.getValue(s))
        ensureThread()
    }

    /** Gọi từ Activity.onStart / onStop. */
    fun onForeground(fg: Boolean) { foreground = fg; refresh() }

    private fun refresh() {
        musicWanted = foreground && musicOn && !am.isMusicActive
        if (musicWanted) ensureThread()
    }

    private fun ensureThread() {
        synchronized(lock) {
            if (thread?.isAlive == true) return
            thread = Thread({ runLoop() }, "pm-audio").apply { isDaemon = true; start() }
        }
    }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            val b = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(maxOf(minBuf, CHUNK * 4 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
            if (Build.VERSION.SDK_INT >= 26) b.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            b.build()
        } catch (_: Exception) { return }
        if (track.state != AudioTrack.STATE_INITIALIZED) { track.release(); return }
        val music = Music()
        val voices = ArrayList<IntArray>()          // [chỉ số trong sfxBufs, vị trí]
        val sfxBufs = ArrayList<FloatArray>()
        val l = FloatArray(CHUNK); val r = FloatArray(CHUNK)
        val out = ShortArray(CHUNK * 2)
        var gain = 0f
        var idle = 0
        try {
            track.play()
            while (foreground) {
                while (true) { val b = pending.poll() ?: break; sfxBufs.add(b); voices.add(intArrayOf(sfxBufs.size - 1, 0)) }
                val target = if (musicWanted) (if (calm) 0.8f else 1.0f) else 0f
                music.calm = calm
                if (gain > 0.0005f || target > 0f) music.render(l, r, CHUNK) else { l.fill(0f); r.fill(0f) }
                // âm lượng nhạc thay đổi mượt (khoảng 1,5 s) để bật / tắt không bị giật
                for (i in 0 until CHUNK) { l[i] *= gain; r[i] *= gain }
                gain += (target - gain) * 0.008f
                if (abs(target - gain) < 0.001f) gain = target
                val it = voices.iterator()
                while (it.hasNext()) {
                    val v = it.next(); val buf = sfxBufs[v[0]]
                    var p = v[1]; var i = 0
                    while (i < CHUNK && p < buf.size) { val s = buf[p] * SFX_GAIN; l[i] += s; r[i] += s; i++; p++ }
                    v[1] = p
                    if (p >= buf.size) it.remove()
                }
                if (voices.isEmpty()) sfxBufs.clear()
                for (i in 0 until CHUNK) {
                    out[2 * i] = toPcm(l[i]); out[2 * i + 1] = toPcm(r[i])
                }
                if (track.write(out, 0, out.size) < 0) break
                idle = if (gain == 0f && target == 0f && voices.isEmpty() && pending.isEmpty()) idle + 1 else 0
                if (idle > SR / CHUNK) break      // im lặng 1 s: dừng luồng, play() / refresh() sẽ bật lại
            }
        } catch (_: Exception) {
        } finally {
            runCatching { track.pause(); track.flush() }
            track.release()
            synchronized(lock) { if (thread === Thread.currentThread()) thread = null }
        }
        // có yêu cầu mới đến đúng lúc luồng vừa thoát
        if (foreground && (musicWanted || pending.isNotEmpty())) ensureThread()
    }

    private fun toPcm(x: Float): Short {
        // giới hạn mềm để vài hiệu ứng chồng nhau không bị vỡ tiếng
        val y = if (x > 0.8f) 0.8f + (x - 0.8f) / (1f + (x - 0.8f) * 5f) else if (x < -0.8f) -0.8f + (x + 0.8f) / (1f - (x + 0.8f) * 5f) else x
        return (y.coerceIn(-1f, 1f) * 32000f).toInt().toShort()
    }

    internal companion object {
        const val SR = 44100
        const val CHUNK = 512
        const val SFX_GAIN = 0.55f
        private const val TN = 4096
        val SINE = FloatArray(TN + 1) { sin(2 * PI * it / TN).toFloat() }
        fun mtof(m: Float): Float = (440.0 * 2.0.pow((m - 69) / 12.0)).toFloat()
        /** sin(2π·phase) bằng bảng tra, phase trong [0, 1). */
        fun sine(phase: Float): Float { val x = phase * TN; val i = x.toInt(); val f = x - i; return SINE[i] + (SINE[i + 1] - SINE[i]) * f }
    }
}

/** Nhạc nền ambient sinh ngẫu nhiên. */
private class Music {
    @Volatile var calm = false
    private val rnd = Random(System.nanoTime())
    private val sr = AudioEngine.SR.toFloat()

    // Các vòng hợp âm (nốt MIDI, gốc ở quãng 3). Mỗi hợp âm kéo dài 2 ô nhịp (8 phách, nhịp 60).
    private val progressions = arrayOf(
        arrayOf(intArrayOf(53, 57, 60, 64), intArrayOf(55, 59, 62, 64), intArrayOf(52, 55, 59, 62), intArrayOf(57, 60, 64, 67)),  // Fmaj7 G6 Em7 Am7
        arrayOf(intArrayOf(57, 60, 64, 71), intArrayOf(53, 57, 60, 67), intArrayOf(48, 55, 62, 64), intArrayOf(55, 59, 62, 69)),  // Am(add9) Fmaj9 Cadd9 G6/9
        arrayOf(intArrayOf(50, 53, 57, 60), intArrayOf(55, 59, 62, 65), intArrayOf(48, 52, 55, 59), intArrayOf(53, 57, 60, 64)),  // Dm7 G7 Cmaj7 Fmaj7
    )
    private val penta = intArrayOf(0, 2, 4, 7, 9)   // ngũ cung trưởng (đô)
    private var key = 0
    private var prog = progressions[0]
    private var chordIdx = -1
    private var samplesToNext = 0
    private var beatSamples = 0
    private val beatLen = (sr * 1.0f).toInt()

    private class Osc(val freq: Float, val amp: Float, val attack: Float, val release: Float, val pan: Float, val decay: Float = 0f) {
        var phase = 0f; var phase2 = 0f; var env = 0f; var releasing = false; var dead = false; var age = 0
    }
    private val pads = ArrayList<Osc>()
    private val plucks = ArrayList<Osc>()

    // delay stereo kiểu ping-pong
    private val dl = FloatArray((sr * 0.75f).toInt()); private val dr = FloatArray((sr * 0.75f).toInt())
    private var dp = 0
    private val delayL = (sr * 0.375f).toInt(); private val delayR = (sr * 0.5f).toInt()
    private var lpL = 0f; private var lpR = 0f

    private fun nextChord() {
        chordIdx++
        if (chordIdx >= prog.size) {
            chordIdx = 0
            prog = progressions[rnd.nextInt(progressions.size)]
            if (rnd.nextInt(4) == 0) key = intArrayOf(0, -2, 2, -3).let { it[rnd.nextInt(it.size)] }
        }
        for (p in pads) p.releasing = true
        val ch = prog[chordIdx]
        for ((i, n) in ch.withIndex()) {
            val f = AudioEngine.mtof((n + key).toFloat())
            val pan = 0.25f + 0.5f * i / (ch.size - 1)
            pads.add(Osc(f * 0.9985f, 0.055f, 2.5f, 3.5f, pan))
            pads.add(Osc(f * 1.0015f, 0.055f, 2.8f, 3.5f, 1f - pan))
        }
        // bass: nốt gốc hạ một quãng tám
        pads.add(Osc(AudioEngine.mtof((ch[0] + key - 12).toFloat()), 0.09f, 1.5f, 3f, 0.5f))
        samplesToNext = beatLen * 8
    }

    private fun maybePluck() {
        val density = if (calm) 0.22f else 0.38f
        if (rnd.nextFloat() >= density) return
        val ch = prog[chordIdx]
        // nốt ngũ cung ở quãng 5, ưu tiên nốt có trong hợp âm
        val base = 72 + key
        val note = if (rnd.nextFloat() < 0.6f) {
            val n = ch[rnd.nextInt(ch.size)]; 72 + key + ((n + key - base) % 12 + 12) % 12
        } else base + penta[rnd.nextInt(penta.size)] + if (rnd.nextInt(3) == 0) 12 else 0
        val f = AudioEngine.mtof(note.toFloat())
        val amp = (if (calm) 0.06f else 0.075f) * (0.7f + 0.3f * rnd.nextFloat())
        plucks.add(Osc(f, amp, 0.004f, 0f, 0.2f + 0.6f * rnd.nextFloat(), decay = 1.1f + rnd.nextFloat()))
        // thỉnh thoảng thêm một nốt lệch nửa phách
        if (!calm && rnd.nextInt(5) == 0) {
            val n2 = base + penta[rnd.nextInt(penta.size)]
            plucks.add(Osc(AudioEngine.mtof(n2.toFloat()), amp * 0.7f, 0.004f, 0f, rnd.nextFloat(), decay = 1.2f).also { it.age = -beatLen / 2 })
        }
    }

    fun render(l: FloatArray, r: FloatArray, n: Int) {
        for (i in 0 until n) {
            if (samplesToNext <= 0) nextChord()
            samplesToNext--
            if (beatSamples <= 0) { beatSamples = beatLen; maybePluck() }
            beatSamples--
            var sl = 0f; var sr0 = 0f
            for (o in pads) {
                if (o.releasing) { o.env -= 1f / (o.release * sr); if (o.env <= 0f) { o.env = 0f; o.dead = true } }
                else if (o.env < 1f) o.env = minOf(1f, o.env + 1f / (o.attack * sr))
                o.phase += o.freq / sr; if (o.phase >= 1f) o.phase -= 1f
                o.phase2 += o.freq * 2f / sr; if (o.phase2 >= 1f) o.phase2 -= 1f
                val s = (AudioEngine.sine(o.phase) + 0.12f * AudioEngine.sine(o.phase2)) * o.amp * o.env * o.env
                sl += s * (1f - o.pan); sr0 += s * o.pan
            }
            for (o in plucks) {
                o.age++
                if (o.age < 0) continue
                val t = o.age / sr
                val env = (if (t < o.attack) t / o.attack else 1f) * exp(-t / o.decay * 2.2f)
                if (t > o.decay * 3f) { o.dead = true; continue }
                o.phase += o.freq / sr; if (o.phase >= 1f) o.phase -= 1f
                o.phase2 += o.freq * 3f / sr; if (o.phase2 >= 1f) o.phase2 -= 1f
                val s = (AudioEngine.sine(o.phase) + 0.18f * AudioEngine.sine(o.phase2) * exp(-t * 6f)) * o.amp * env
                sl += s * (1f - o.pan); sr0 += s * o.pan
            }
            // lọc thông thấp nhẹ cho ấm
            lpL += (sl - lpL) * 0.35f; lpR += (sr0 - lpR) * 0.35f
            val rl = dl[(dp - delayL + dl.size) % dl.size]; val rr = dr[(dp - delayR + dr.size) % dr.size]
            dl[dp] = lpL + rr * 0.38f; dr[dp] = lpR + rl * 0.38f
            dp = (dp + 1) % dl.size
            l[i] = lpL + rl * 0.32f; r[i] = lpR + rr * 0.32f
        }
        pads.removeAll { it.dead }
        plucks.removeAll { it.dead }
    }
}

/** Kết xuất sẵn các hiệu ứng (mono, float). */
private object SfxBank {
    private val sr = AudioEngine.SR.toFloat()

    fun render(): Map<Sfx, FloatArray> = mapOf(
        Sfx.MOVE to wood(),
        Sfx.CHECK to notes(listOf(0f to 81f, 0.09f to 88f), 0.55f, bell = true, amp = 0.45f),
        Sfx.WIN to notes(listOf(0.12f to 72f, 0.22f to 76f, 0.32f to 79f, 0.42f to 84f, 0.42f to 88f), 1.1f, amp = 0.42f),
        Sfx.LOSE to notes(listOf(0.12f to 67f, 0.34f to 63f, 0.56f to 60f, 0.56f to 55f), 1.2f, amp = 0.36f),
        Sfx.DRAW to notes(listOf(0.12f to 69f, 0.36f to 76f, 0.36f to 64f), 1.0f, amp = 0.36f),
        Sfx.REWARD to notes(listOf(0f to 84f, 0.06f to 88f, 0.12f to 91f, 0.18f to 95f, 0.24f to 96f), 0.7f, bell = true, amp = 0.3f),
        Sfx.TAP to tick(),
    )

    /** Tiếng đặt quân gỗ: thân trầm hạ cao độ nhanh + tiếng gõ nhiễu ngắn. */
    private fun wood(): FloatArray {
        val n = (sr * 0.12f).toInt(); val a = FloatArray(n); val rnd = Random(7)
        var ph = 0f; var lp = 0f; var hp = 0f; var prev = 0f
        for (i in 0 until n) {
            val t = i / sr
            val f = 140f + 160f * exp(-t * 60f)
            ph += f / sr; if (ph >= 1f) ph -= 1f
            val body = AudioEngine.sine(ph) * exp(-t * 38f)
            val w = rnd.nextFloat() * 2f - 1f
            hp = 0.85f * (hp + w - prev); prev = w
            lp += (hp - lp) * 0.45f
            val click = lp * exp(-t * 260f)
            a[i] = (body * 0.75f + click * 0.9f) * 0.9f
        }
        return a
    }

    private fun tick(): FloatArray {
        val n = (sr * 0.03f).toInt(); val a = FloatArray(n); var ph = 0f
        for (i in 0 until n) { val t = i / sr; ph += 1800f / sr; if (ph >= 1f) ph -= 1f; a[i] = AudioEngine.sine(ph) * exp(-t * 180f) * 0.35f }
        return a
    }

    /** Chuỗi nốt (thời điểm s, nốt MIDI) kiểu kalimba / chuông. */
    private fun notes(seq: List<Pair<Float, Float>>, decay: Float, bell: Boolean = false, amp: Float): FloatArray {
        val len = seq.maxOf { it.first } + decay * 2.2f
        val a = FloatArray((sr * len).toInt())
        for ((start, m) in seq) {
            val f = AudioEngine.mtof(m); val s0 = (start * sr).toInt()
            var p1 = 0f; var p2 = 0f; var p3 = 0f
            val r2 = if (bell) 2.76f else 2f; val r3 = if (bell) 5.4f else 3f
            for (i in s0 until a.size) {
                val t = (i - s0) / sr
                val env = minOf(1f, t / 0.003f) * exp(-t / decay * 2.5f)
                p1 += f / sr; if (p1 >= 1f) p1 -= 1f
                p2 += f * r2 / sr; while (p2 >= 1f) p2 -= 1f
                p3 += f * r3 / sr; while (p3 >= 1f) p3 -= 1f
                a[i] += (AudioEngine.sine(p1) + 0.35f * AudioEngine.sine(p2) * exp(-t * 5f) + 0.15f * AudioEngine.sine(p3) * exp(-t * 12f)) * env * amp
            }
        }
        // tắt dần cuối bộ đệm để không có tiếng tách
        val fade = (sr * 0.05f).toInt()
        for (k in 0 until fade) a[a.size - 1 - k] *= k / fade.toFloat()
        return a
    }
}
