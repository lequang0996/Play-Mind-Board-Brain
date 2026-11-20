package com.fws.playmind.net

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Chơi qua internet bằng WebRTC DataChannel. Không có máy chủ signaling: hai người trao đổi
 * mã phòng (offer/answer) bằng cách sao chép hoặc quét QR. Dùng STUN công cộng của Google;
 * một số mạng (NAT đối xứng) có thể không kết nối được vì không có TURN.
 */
class WebRtcTransport(private val ctx: Context) : Transport {
    companion object {
        private var factory: PeerConnectionFactory? = null
        private fun factory(ctx: Context): PeerConnectionFactory {
            factory?.let { return it }
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(ctx.applicationContext).createInitializationOptions())
            return PeerConnectionFactory.builder().createPeerConnectionFactory().also { factory = it }
        }
    }

    private val _state = MutableStateFlow(LinkState.IDLE)
    override val state: StateFlow<LinkState> = _state
    override var onMessage: ((String) -> Unit)? = null

    private var pc: PeerConnection? = null
    private var dc: DataChannel? = null
    private var gathered = CompletableDeferred<Unit>()

    private open class SdpObs : SdpObserver {
        override fun onCreateSuccess(d: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(e: String?) {}
        override fun onSetFailure(e: String?) {}
    }

    private val dcObserver = object : DataChannel.Observer {
        override fun onBufferedAmountChange(previous: Long) {}
        override fun onStateChange() {
            when (dc?.state()) {
                DataChannel.State.OPEN -> _state.value = LinkState.CONNECTED
                DataChannel.State.CLOSED, DataChannel.State.CLOSING -> if (_state.value != LinkState.FAILED) _state.value = LinkState.CLOSED
                else -> {}
            }
        }
        override fun onMessage(buffer: DataChannel.Buffer?) {
            val b = buffer?.data ?: return
            val bytes = ByteArray(b.remaining()); b.get(bytes)
            onMessage?.invoke(String(bytes, Charsets.UTF_8))
        }
    }

    private val pcObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
            when (s) {
                PeerConnection.IceConnectionState.FAILED -> _state.value = LinkState.FAILED
                PeerConnection.IceConnectionState.CLOSED -> if (_state.value != LinkState.FAILED) _state.value = LinkState.CLOSED
                else -> {}
            }
        }
        override fun onIceConnectionReceivingChange(b: Boolean) {}
        override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {
            if (s == PeerConnection.IceGatheringState.COMPLETE) gathered.complete(Unit)
        }
        override fun onIceCandidate(c: IceCandidate?) {}
        override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
        override fun onAddStream(s: MediaStream?) {}
        override fun onRemoveStream(s: MediaStream?) {}
        override fun onDataChannel(ch: DataChannel?) {
            dc = ch; ch?.registerObserver(dcObserver)
        }
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(r: RtpReceiver?, s: Array<out MediaStream>?) {}
    }

    private fun newPc(): PeerConnection {
        val servers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        )
        val cfg = PeerConnection.RTCConfiguration(servers).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
        gathered = CompletableDeferred()
        return factory(ctx).createPeerConnection(cfg, pcObserver) ?: error("PeerConnection")
    }

    private suspend fun PeerConnection.offerS(): SessionDescription = suspendCancellableCoroutine { c ->
        createOffer(object : SdpObs() {
            override fun onCreateSuccess(d: SessionDescription?) { if (d != null) c.resume(d) else c.resumeWithException(IllegalStateException("offer")) }
            override fun onCreateFailure(e: String?) { c.resumeWithException(IllegalStateException(e ?: "offer")) }
        }, MediaConstraints())
    }

    private suspend fun PeerConnection.answerS(): SessionDescription = suspendCancellableCoroutine { c ->
        createAnswer(object : SdpObs() {
            override fun onCreateSuccess(d: SessionDescription?) { if (d != null) c.resume(d) else c.resumeWithException(IllegalStateException("answer")) }
            override fun onCreateFailure(e: String?) { c.resumeWithException(IllegalStateException(e ?: "answer")) }
        }, MediaConstraints())
    }

    private suspend fun PeerConnection.setLocalS(d: SessionDescription) = suspendCancellableCoroutine<Unit> { c ->
        setLocalDescription(object : SdpObs() {
            override fun onSetSuccess() { c.resume(Unit) }
            override fun onSetFailure(e: String?) { c.resumeWithException(IllegalStateException(e ?: "local")) }
        }, d)
    }

    private suspend fun PeerConnection.setRemoteS(d: SessionDescription) = suspendCancellableCoroutine<Unit> { c ->
        setRemoteDescription(object : SdpObs() {
            override fun onSetSuccess() { c.resume(Unit) }
            override fun onSetFailure(e: String?) { c.resumeWithException(IllegalStateException(e ?: "remote")) }
        }, d)
    }

    /** Chủ phòng: tạo mã mời (offer). */
    suspend fun createInvite(): String {
        val p = newPc(); pc = p
        dc = p.createDataChannel("pm", DataChannel.Init().apply { ordered = true })
        dc?.registerObserver(dcObserver)
        p.setLocalS(p.offerS())
        withTimeoutOrNull(6000) { gathered.await() }
        return Signal.encode('o', p.localDescription.description)
    }

    /** Chủ phòng: nhận mã trả lời (answer) của khách. Trả về false nếu mã sai. */
    suspend fun acceptReply(code: String): Boolean {
        val sdp = Signal.decode('a', code) ?: return false
        val p = pc ?: return false
        _state.value = LinkState.CONNECTING
        return try { p.setRemoteS(SessionDescription(SessionDescription.Type.ANSWER, sdp)); true } catch (e: Exception) { _state.value = LinkState.FAILED; false }
    }

    /** Khách: nhận mã mời, trả về mã trả lời. null nếu mã sai. */
    suspend fun createReply(inviteCode: String): String? {
        val sdp = Signal.decode('o', inviteCode) ?: return null
        val p = newPc(); pc = p
        _state.value = LinkState.CONNECTING
        return try {
            p.setRemoteS(SessionDescription(SessionDescription.Type.OFFER, sdp))
            p.setLocalS(p.answerS())
            withTimeoutOrNull(6000) { gathered.await() }
            Signal.encode('a', p.localDescription.description)
        } catch (e: Exception) { _state.value = LinkState.FAILED; null }
    }

    override fun send(text: String) {
        val ch = dc ?: return
        if (ch.state() == DataChannel.State.OPEN) ch.send(DataChannel.Buffer(ByteBuffer.wrap(text.toByteArray(Charsets.UTF_8)), false))
    }

    override fun close() {
        try { dc?.close(); pc?.close(); dc?.dispose(); pc?.dispose() } catch (_: Exception) {}
        dc = null; pc = null
        if (_state.value != LinkState.FAILED) _state.value = LinkState.CLOSED
    }
}
