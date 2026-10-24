package com.barryg.spotifywidget

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat

object SpotifyController {
    const val PACKAGE = "com.spotify.music"

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun controller(context: Context): MediaController? {
        if (!hasAccess(context)) return null
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, SpotifyNotificationListener::class.java)
        return try {
            manager.getActiveSessions(listener).firstOrNull { it.packageName == PACKAGE }
        } catch (e: SecurityException) {
            null
        }
    }

    fun previous(context: Context) = send(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS) { skipToPrevious() }

    fun next(context: Context) = send(context, KeyEvent.KEYCODE_MEDIA_NEXT) { skipToNext() }

    fun playPause(context: Context) = send(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
        if (controller(context)?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) pause() else play()
    }

    private fun send(
        context: Context,
        keyCode: Int,
        viaSession: MediaController.TransportControls.() -> Unit,
    ) {
        val session = controller(context)
        if (session != null) {
            session.transportControls.viaSession()
            return
        }
        // No Spotify session: fall back to a global media key.
        val audio = context.getSystemService(AudioManager::class.java)
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
