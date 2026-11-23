package com.fws.playmind.net

import android.Manifest
import android.content.Context
import android.os.Build
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FoundRoom(val endpointId: String, val host: String)

/** Chơi gần nhau bằng Google Nearby Connections (Bluetooth / Wi-Fi), không cần internet. */
class NearbyTransport(ctx: Context, private val gameId: String) : Transport {
    companion object {
        private const val SERVICE = "com.fws.playmind"

        /** Danh sách quyền runtime cần xin cho Nearby theo phiên bản Android. */
        fun requiredPermissions(): Array<String> = buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE); add(Manifest.permission.BLUETOOTH_CONNECT); add(Manifest.permission.BLUETOOTH_SCAN)
            }
            // Android 12 trở xuống vẫn cần quyền vị trí cho Bluetooth/Wi-Fi quét (khớp với manifest: tối đa API 31)
            if (Build.VERSION.SDK_INT <= 31) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }.toTypedArray()

        // Mã trạng thái "đang phát/đang tìm rồi" không phải lỗi thật (gọi lặp), bỏ qua.
        private const val ALREADY_ADVERTISING = 8001
        private const val ALREADY_DISCOVERING = 8002
    }

    private val client: ConnectionsClient = Nearby.getConnectionsClient(ctx.applicationContext)
    private val _state = MutableStateFlow(LinkState.IDLE)
    override val state: StateFlow<LinkState> = _state
    override var onMessage: ((String) -> Unit)? = null

    private val _rooms = MutableStateFlow<List<FoundRoom>>(emptyList())
    val rooms: StateFlow<List<FoundRoom>> = _rooms
    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error
    /** Mã lỗi gần nhất của Nearby (để hiển thị cho người dùng/nhà phát triển tra cứu). */
    private val _errorCode = MutableStateFlow(0)
    val errorCode: StateFlow<Int> = _errorCode

    private fun fail(e: Exception) {
        val code = (e as? ApiException)?.statusCode ?: -1
        if (code == ALREADY_ADVERTISING || code == ALREADY_DISCOVERING) return
        _errorCode.value = code
        _error.value = true
    }

    private var peer: String? = null

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { onMessage?.invoke(String(it, Charsets.UTF_8)) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _state.value = LinkState.CONNECTING
            client.acceptConnection(endpointId, payloads)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                peer = endpointId
                client.stopAdvertising(); client.stopDiscovery()
                _state.value = LinkState.CONNECTED
            } else _state.value = LinkState.FAILED
        }
        override fun onDisconnected(endpointId: String) {
            if (endpointId == peer) _state.value = LinkState.CLOSED
        }
    }

    /** Chủ phòng: phát sóng để người khác tìm thấy. */
    fun host(nickname: String) {
        val opts = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        client.startAdvertising("$gameId|$nickname", SERVICE, lifecycle, opts).addOnFailureListener { fail(it) }
    }

    /** Khách: tìm các phòng cùng game ở gần. */
    fun discover() {
        val cb = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                val parts = info.endpointName.split("|", limit = 2)
                if (parts.size == 2 && parts[0] == gameId && _rooms.value.none { it.endpointId == endpointId })
                    _rooms.value = _rooms.value + FoundRoom(endpointId, parts[1])
            }
            override fun onEndpointLost(endpointId: String) { _rooms.value = _rooms.value.filter { it.endpointId != endpointId } }
        }
        val opts = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        client.startDiscovery(SERVICE, cb, opts).addOnFailureListener { fail(it) }
    }

    fun connect(room: FoundRoom, nickname: String) {
        _state.value = LinkState.CONNECTING
        client.requestConnection("$gameId|$nickname", room.endpointId, lifecycle).addOnFailureListener { _state.value = LinkState.FAILED }
    }

    override fun send(text: String) {
        peer?.let { client.sendPayload(it, Payload.fromBytes(text.toByteArray(Charsets.UTF_8))) }
    }

    override fun close() {
        try { client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints() } catch (_: Exception) {}
        if (_state.value != LinkState.FAILED) _state.value = LinkState.CLOSED
    }
}
