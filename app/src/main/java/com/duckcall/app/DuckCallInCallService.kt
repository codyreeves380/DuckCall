package com.duckcall.app

import android.app.InCallService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared state between Telecom's InCallService and DuckCall's Compose UI.
 * Telecom owns the call connection; this class only exposes state and controls.
 */
object ActiveCallStore {
    var call: Call? by mutableStateOf(null)
        private set
    var state: Int by mutableStateOf(Call.STATE_NEW)
        private set
    var phoneNumber: String by mutableStateOf("")
        private set
    var muted: Boolean by mutableStateOf(false)
        private set
    var speaker: Boolean by mutableStateOf(false)
        private set

    private var callback: Call.Callback? = null
    private var inCallService: DuckCallInCallService? = null

    fun attach(service: DuckCallInCallService, newCall: Call) {
        inCallService = service
        detachCallback()
        call = newCall
        state = newCall.state
        phoneNumber = newCall.details.handle?.schemeSpecificPart.orEmpty()
        val listener = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                state = newState
                if (newState == Call.STATE_DISCONNECTED) clear(call)
            }
            override fun onDetailsChanged(call: Call, details: Call.Details) {
                phoneNumber = details.handle?.schemeSpecificPart.orEmpty()
            }
        }
        callback = listener
        newCall.registerCallback(listener)
    }

    fun detach(service: DuckCallInCallService, oldCall: Call) {
        if (call === oldCall) clear(oldCall)
        if (inCallService === service) inCallService = null
    }

    private fun detachCallback() {
        val oldCall = call
        val oldCallback = callback
        if (oldCall != null && oldCallback != null) oldCall.unregisterCallback(oldCallback)
        callback = null
    }

    private fun clear(expected: Call) {
        if (call !== expected) return
        detachCallback()
        call = null
        state = Call.STATE_DISCONNECTED
        phoneNumber = ""
        muted = false
        speaker = false
    }

    fun answer() { call?.answer(VideoProfile.STATE_AUDIO_ONLY) }
    fun hangUp() { call?.disconnect() }
    fun hold() { call?.hold() }
    fun unhold() { call?.unhold() }

    fun toggleMute() {
        muted = !muted
        inCallService?.setMuted(muted)
    }

    fun toggleSpeaker() {
        speaker = !speaker
        inCallService?.setAudioRoute(
            if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        )
    }
}

class DuckCallInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        ActiveCallStore.attach(this, call)
        showCallNotification(call)
    }

    override fun onCallRemoved(call: Call) {
        ActiveCallStore.detach(this, call)
        getSystemService(NotificationManager::class.java).cancel(CALL_NOTIFICATION_ID)
        super.onCallRemoved(call)
    }

    private fun showCallNotification(call: Call) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CALL_CHANNEL_ID,
                    "Phone calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming and ongoing DuckCall calls"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }

        val openApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 100, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val caller = call.details.handle?.schemeSpecificPart ?: "Unknown caller"
        val notification = Notification.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(if (call.state == Call.STATE_RINGING) "Incoming call" else "DuckCall")
            .setContentText(caller)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(call.state != Call.STATE_DISCONNECTED)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()
        manager.notify(CALL_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CALL_CHANNEL_ID = "duckcall_calls"
        private const val CALL_NOTIFICATION_ID = 4101
    }
}