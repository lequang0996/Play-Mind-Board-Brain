package com.fws.playmind.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Nhạc nền của app: ambient thư giãn để tập trung sâu, **tổng hợp hoàn toàn bằng mã** (không có tệp nhạc, không lo bản
 * quyền, không tăng dung lượng APK), sinh ngẫu nhiên nên không bao giờ lặp y hệt.
 *
 * Pad hợp âm 7 / 9 vào ra rất chậm (mỗi hợp âm 16 s), bass trầm, thỉnh thoảng một nốt đàn mềm trên âm giai ngũ cung,
 * lớp nhiễu nâu rất nhỏ (như mưa xa) che tiếng ồn xung quanh, delay stereo cho không gian. Không có nhịp trống, không có
 * giai điệu nổi bật. Trong ván ([calm] = true) nốt đàn thưa hơn và âm lượng thấp hơn ở menu.
 *
 * Một luồng ghi vào một [AudioTrack], chỉ chạy khi app ở tiền cảnh và nhạc được bật; app khác đang phát nhạc thì không bật.
 */
class AudioEngine(ctx: Context) {
    private val sp = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var musicOn by mutableStateOf(sp.getBoolean("music_on", true)); private set

    /** true khi đang trong ván: nhạc thưa và dịu hơn. */
    @Volatile var calm = false

    @Volatile private var foreground = false
    @Volatile private var musicWanted = false
    private var thread: Thread? = null
    private val lock = Any()

    fun updateMusic(v: Boolean) { musicOn = v; sp.edit().putBoolean("music_on", v).apply(); refresh() }

    /** Gọi từ Activity.onStart / onStop. */
    fun onForeground(fg: Boolean) { foreground = fg; refresh() }

    private fun refresh() {
        musicWanted = foreground && musicOn && !am.isMusicActive
        if (musicWanted) ensureThread()
    }

    private fun ensureThread() {
        synchronized(lock) {
            if (thread?.isAlive == true) return
            thread = Thread({ runLoop() }, "pm-music").apply { isDaemon = true; start() }
        }
    }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(maxOf(minBuf, CHUNK * 4 * 8))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) { return }
        if (track.state != AudioTrack.STATE_INITIALIZED) { track.release(); return }
        val music = Music()
        val l = FloatArray(CHUNK); val r = FloatArray(CHUNK)
        val out = ShortArray(CHUNK * 2)
        var gain = 0f
        try {
            track.play()
            while (true) {
                val target = if (musicWanted) (if (calm) 0.8f else 1.0f) else 0f
                if (target == 0f && gain < 0.002f) break          // đã tắt hẳn: dừng luồng, refresh() sẽ bật lại
                music.calm = calm
                music.render(l, r, CHUNK)
                // âm lượng thay đổi mượt (khoảng 1,5 s) để bật / tắt không bị giật
                for (i in 0 until CHUNK) {
                    out[2 * i] = (l[i] * gain).coerceIn(-1f, 1f).times(32000f).toInt().toShort()
                    out[2 * i + 1] = (r[i] * gain).coerceIn(-1f, 1f).times(32000f).toInt().toShort()
                }
                gain += (target - gain) * 0.008f
                if (abs(target - gain) < 0.001f) gain = target
                if (track.write(out, 0, out.size) < 0) break
            }
        } catch (_: Exception) {
        } finally {
            runCatching { track.pause(); track.flush() }
            track.release()
            synchronized(lock) { if (thread === Thread.currentThread()) thread = null }
        }
        if (musicWanted) ensureThread()      // được bật lại đúng lúc luồng vừa thoát
    }

    internal companion object {
        const val SR = 44100
        const val CHUNK = 512
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

    // Các vòng hợp âm (nốt MIDI, gốc ở quãng 3), toàn hợp âm mở, không có quãng nghịch mạnh.
    private val progressions = arrayOf(
        arrayOf(intArrayOf(53, 57, 60, 64), intArrayOf(55, 59, 62, 64), intArrayOf(52, 55, 59, 62), intArrayOf(57, 60, 64, 67)),  // Fmaj7 G6 Em7 Am7
        arrayOf(intArrayOf(57, 60, 64, 71), intArrayOf(53, 57, 60, 67), intArrayOf(48, 55, 62, 64), intArrayOf(55, 59, 62, 69)),  // Am(add9) Fmaj9 Cadd9 G6/9
        arrayOf(intArrayOf(50, 57, 60, 64), intArrayOf(53, 57, 60, 64), intArrayOf(48, 55, 59, 64), intArrayOf(53, 60, 64, 67)),  // Dm9 Fmaj7 Cmaj7 Fadd9
    )
    private val penta = intArrayOf(0, 2, 4, 7, 9)   // ngũ cung trưởng (đô)
    private var key = 0
    private var prog = progressions[0]
    private var chordIdx = -1
    private var samplesToNext = 0
    private var slotSamples = 0
    private val slotLen = (sr * 2f).toInt()          // mỗi 2 s có thể có một nốt đàn
    private var t = 0L

    private class Osc(val freq: Float, val amp: Float, val attack: Float, val release: Float, val pan: Float, val decay: Float = 0f) {
        var phase = 0f; var phase2 = 0f; var env = 0f; var releasing = false; var dead = false; var age = 0
    }
    private val pads = ArrayList<Osc>()
    private val notes = ArrayList<Osc>()

    // delay stereo kiểu ping-pong, phản hồi dài cho cảm giác phòng rộng
    private val dl = FloatArray((sr * 0.9f).toInt()); private val dr = FloatArray((sr * 0.9f).toInt())
    private var dp = 0
    private val delayL = (sr * 0.55f).toInt(); private val delayR = (sr * 0.8f).toInt()
    private var lpL = 0f; private var lpR = 0f
    private var brown = 0f; private var brownLp = 0f

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
            pads.add(Osc(f * 0.9988f, 0.05f, 5f, 7f, pan))
            pads.add(Osc(f * 1.0012f, 0.05f, 5.5f, 7f, 1f - pan))
        }
        pads.add(Osc(AudioEngine.mtof((ch[0] + key - 12).toFloat()), 0.085f, 4f, 7f, 0.5f))   // bass
        samplesToNext = (sr * 16f).toInt()
    }

    private fun maybeNote() {
        if (rnd.nextFloat() >= (if (calm) 0.28f else 0.42f)) return
        val ch = prog[chordIdx]
        val base = 67 + key
        // ưu tiên nốt trong hợp âm, quãng 4-5, không lên quá cao
        val note = if (rnd.nextFloat() < 0.7f) {
            val n = ch[rnd.nextInt(ch.size)]; base + ((n + key - base) % 12 + 12) % 12
        } else base + penta[rnd.nextInt(penta.size)]
        val amp = (if (calm) 0.045f else 0.06f) * (0.6f + 0.4f * rnd.nextFloat())
        notes.add(Osc(AudioEngine.mtof(note.toFloat()), amp, 0.02f, 0f, 0.25f + 0.5f * rnd.nextFloat(), decay = 2.2f + rnd.nextFloat()).also {
            it.age = -rnd.nextInt(slotLen / 2)        // lệch thời điểm cho tự nhiên
        })
    }

    fun render(l: FloatArray, r: FloatArray, n: Int) {
        for (i in 0 until n) {
            if (samplesToNext <= 0) nextChord()
            samplesToNext--
            if (slotSamples <= 0) { slotSamples = slotLen; maybeNote() }
            slotSamples--
            t++
            // "thở" rất chậm (chu kỳ ~20 s) cho pad không đứng im
            val breath = 0.85f + 0.15f * AudioEngine.sine(((t % (sr * 20f).toLong()) / (sr * 20f)))
            var sl = 0f; var sr0 = 0f
            for (o in pads) {
                if (o.releasing) { o.env -= 1f / (o.release * sr); if (o.env <= 0f) { o.env = 0f; o.dead = true } }
                else if (o.env < 1f) o.env = minOf(1f, o.env + 1f / (o.attack * sr))
                o.phase += o.freq / sr; if (o.phase >= 1f) o.phase -= 1f
                o.phase2 += o.freq * 2f / sr; if (o.phase2 >= 1f) o.phase2 -= 1f
                val s = (AudioEngine.sine(o.phase) + 0.08f * AudioEngine.sine(o.phase2)) * o.amp * o.env * o.env * breath
                sl += s * (1f - o.pan); sr0 += s * o.pan
            }
            for (o in notes) {
                o.age++
                if (o.age < 0) continue
                val tt = o.age / sr
                if (tt > o.decay * 3f) { o.dead = true; continue }
                val env = (if (tt < o.attack) tt / o.attack else 1f) * exp(-tt / o.decay * 2f)
                o.phase += o.freq / sr; if (o.phase >= 1f) o.phase -= 1f
                o.phase2 += o.freq * 2f / sr; if (o.phase2 >= 1f) o.phase2 -= 1f
                val s = (AudioEngine.sine(o.phase) + 0.15f * AudioEngine.sine(o.phase2) * exp(-tt * 3f)) * o.amp * env
                sl += s * (1f - o.pan); sr0 += s * o.pan
            }
            // nhiễu nâu rất nhỏ, lọc thấp: như mưa xa
            brown = (brown + (rnd.nextFloat() * 2f - 1f) * 0.02f) * 0.998f
            brownLp += (brown - brownLp) * 0.05f
            val air = brownLp * (if (calm) 0.10f else 0.12f)
            // lọc thông thấp cho ấm, không chói
            lpL += (sl - lpL) * 0.22f; lpR += (sr0 - lpR) * 0.22f
            val rl = dl[(dp - delayL + dl.size) % dl.size]; val rr = dr[(dp - delayR + dr.size) % dr.size]
            dl[dp] = lpL + rr * 0.45f; dr[dp] = lpR + rl * 0.45f
            dp = (dp + 1) % dl.size
            l[i] = lpL + rl * 0.35f + air; r[i] = lpR + rr * 0.35f + air
        }
        pads.removeAll { it.dead }
        notes.removeAll { it.dead }
    }
}
