package com.fws.playmind.net

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Chủ phòng nhiều người (3-4 người) qua Nearby: phát sóng và nhận tối đa [maxPeers] khách cùng lúc.
 * Mỗi khách là một [Link] (một [Transport] riêng); khách vẫn dùng [NearbyTransport] bình thường để tìm và kết nối.
 */
class NearbyHub(ctx: Context, private val gameId: String, private val maxPeers: Int) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(ctx.applicationContext)

    inner class Link(val endpointId: String) : Transport {
        internal val st = MutableStateFlow(LinkState.CONNECTED)
        override val state: StateFlow<LinkState> = st
        override var onMessage: ((String) -> Unit)? = null
        override fun send(text: String) {
            if (st.value == LinkState.CONNECTED) client.sendPayload(endpointId, Payload.fromBytes(text.toByteArray(Charsets.UTF_8)))
        }
        override fun close() {
            try { client.disconnectFromEndpoint(endpointId) } catch (_: Exception) {}
            links.remove(endpointId)
            st.value = LinkState.CLOSED
            resumeIfRoom()
        }
    }

    /** Gọi mỗi khi có một khách kết nối thành công. */
    var onPeer: ((Link) -> Unit)? = null
    private val links = LinkedHashMap<String, Link>()
    private var nickname = ""
    private var advertising = false
    private var accepting = true

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error
    private val _errorCode = MutableStateFlow(0)
    val errorCode: StateFlow<Int> = _errorCode

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val text = payload.asBytes()?.let { String(it, Charsets.UTF_8) } ?: return
            links[endpointId]?.onMessage?.invoke(text)
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if (!accepting || links.size >= maxPeers) client.rejectConnection(endpointId)
            else client.acceptConnection(endpointId, payloads)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (!result.status.isSuccess) return
            val l = Link(endpointId); links[endpointId] = l
            if (links.size >= maxPeers) pause()
            onPeer?.invoke(l)
        }
        override fun onDisconnected(endpointId: String) {
            links.remove(endpointId)?.st?.value = LinkState.CLOSED
            resumeIfRoom()
        }
    }

    private fun fail(e: Exception) {
        val code = (e as? ApiException)?.statusCode ?: -1
        if (code == 8001) return                       // đang phát sóng rồi
        _errorCode.value = code; _error.value = true
    }

    /** Bắt đầu phát sóng phòng. */
    fun start(nickname: String) {
        this.nickname = nickname; accepting = true
        advertise()
    }

    private fun advertise() {
        if (advertising) return
        advertising = true
        val opts = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        client.startAdvertising("$gameId|$nickname", "com.fws.playmind", lifecycle, opts).addOnFailureListener { advertising = false; fail(it) }
    }

    private fun pause() { if (advertising) { advertising = false; try { client.stopAdvertising() } catch (_: Exception) {} } }

    private fun resumeIfRoom() { if (accepting && links.size < maxPeers) advertise() }

    /** Dừng nhận thêm khách (khi ván bắt đầu). */
    fun lock() { accepting = false; pause() }

    fun close() {
        accepting = false
        try { client.stopAdvertising(); client.stopAllEndpoints() } catch (_: Exception) {}
        advertising = false
        for (l in links.values) l.st.value = LinkState.CLOSED
        links.clear()
    }
}
