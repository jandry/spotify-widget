package com.barryg.spotifywidget

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/** Only exists to be granted notification access, which unlocks MediaSessionManager. */
class SpotifyNotificationListener : NotificationListenerService() {

    private var tracked: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { track(it) }

    private val component get() = ComponentName(this, SpotifyNotificationListener::class.java)

    override fun onListenerConnected() {
        val manager = getSystemService(MediaSessionManager::class.java)
        manager.addOnActiveSessionsChangedListener(sessionsListener, component)
        track(manager.getActiveSessions(component))
    }

    override fun onListenerDisconnected() {
        getSystemService(MediaSessionManager::class.java).removeOnActiveSessionsChangedListener(sessionsListener)
        tracked?.unregisterCallback(callback)
        tracked = null
    }

    private fun track(controllers: List<MediaController>?) {
        val spotify = controllers?.firstOrNull { it.packageName == SpotifyController.PACKAGE }
        if (spotify?.sessionToken != tracked?.sessionToken) {
            tracked?.unregisterCallback(callback)
            spotify?.registerCallback(callback)
            tracked = spotify
        }
        refresh()
    }

    private fun refresh() = SpotifyWidgetProvider.refresh(this)
}
