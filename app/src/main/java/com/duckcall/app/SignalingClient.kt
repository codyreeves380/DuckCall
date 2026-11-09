package com.duckcall.app

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal WebSocket signaling transport for the DuckCall WebRTC prototype.
 *
 * The endpoint must be a secure wss:// URL in deployed builds. This class only
 * transports call setup messages; it does not send or receive voice media.
 */
class SignalingClient(
    private val endpoint: String,
    private val listener: Listener
) {
    interface Listener {
        fun onConnected(clientId: String?)
        fun onEvent(type: String, payload: JSONObject)
        fun onFailure(message: String)
        fun onClosed()
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var socket: WebSocket? = null

    fun connect() {
        if (!endpoint.startsWith("wss://")) {
            listener.onFailure("A secure wss:// signaling endpoint is required.")
            return
        }

        val request = Request.Builder().url(endpoint).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = JSONObject(text)
                    val type = message.optString("type")
                    if (type == "connected") {
                        listener.onConnected(message.optString("clientId").ifBlank { null })
                    } else {
                        listener.onEvent(type, message)
                    }
                } catch (_: Exception) {
                    listener.onFailure("The signaling server sent an invalid message.")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                listener.onFailure(t.message ?: "Signaling connection failed.")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                listener.onClosed()
            }
        })
    }

    fun joinRoom(roomId: String): Boolean =
        send(JSONObject().put("type", "join").put("roomId", roomId))

    fun sendOffer(payload: JSONObject): Boolean = sendSignal("offer", payload)

    fun sendAnswer(payload: JSONObject): Boolean = sendSignal("answer", payload)

    fun sendIceCandidate(payload: JSONObject): Boolean = sendSignal("ice-candidate", payload)

    fun leave(): Boolean = send(JSONObject().put("type", "leave"))

    fun close() {
        socket?.close(1000, "Client closed")
        socket = null
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun sendSignal(type: String, payload: JSONObject): Boolean =
        send(JSONObject().put("type", type).put("payload", payload))

    private fun send(message: JSONObject): Boolean {
        val current = socket ?: return false
        return current.send(message.toString())
    }
}
