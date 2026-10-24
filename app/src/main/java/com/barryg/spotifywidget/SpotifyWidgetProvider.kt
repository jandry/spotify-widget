package com.barryg.spotifywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.provider.Settings
import android.widget.RemoteViews

/** 4×1 variant: same behaviour and layout, declared separately so it shows up as its own widget. */
class SpotifyWidgetProvider4x1 : SpotifyWidgetProvider()

open class SpotifyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = buildViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREVIOUS -> SpotifyController.previous(context)
            ACTION_PLAY_PAUSE -> SpotifyController.playPause(context)
            ACTION_NEXT -> SpotifyController.next(context)
        }
    }

    companion object {
        private const val ACTION_PREVIOUS = "com.barryg.spotifywidget.PREVIOUS"
        private const val ACTION_PLAY_PAUSE = "com.barryg.spotifywidget.PLAY_PAUSE"
        private const val ACTION_NEXT = "com.barryg.spotifywidget.NEXT"
        private const val ART_SIZE_PX = 128

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = listOf(SpotifyWidgetProvider::class.java, SpotifyWidgetProvider4x1::class.java)
                .flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }
                .toIntArray()
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_spotify)
            val hasAccess = SpotifyController.hasAccess(context)
            val controller = SpotifyController.controller(context)
            val metadata = controller?.metadata
            val playing = controller?.playbackState?.state == PlaybackState.STATE_PLAYING

            views.setTextViewText(
                R.id.title,
                when {
                    !hasAccess -> context.getString(R.string.widget_grant_access)
                    metadata == null -> context.getString(R.string.widget_nothing)
                    else -> metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                },
            )
            views.setTextViewText(R.id.artist, metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty())
            views.setImageViewResource(R.id.btn_play, if (playing) R.drawable.ic_pause else R.drawable.ic_play)

            val art = metadata?.let {
                it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: it.getBitmap(MediaMetadata.METADATA_KEY_ART)
            }
            if (art != null) {
                views.setImageViewBitmap(R.id.art, Bitmap.createScaledBitmap(art, ART_SIZE_PX, ART_SIZE_PX, true))
            } else {
                views.setImageViewResource(R.id.art, android.R.color.transparent)
            }

            views.setOnClickPendingIntent(R.id.btn_prev, actionIntent(context, ACTION_PREVIOUS))
            views.setOnClickPendingIntent(R.id.btn_play, actionIntent(context, ACTION_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.btn_next, actionIntent(context, ACTION_NEXT))

            val open = openIntent(context, hasAccess)
            views.setOnClickPendingIntent(R.id.art, open)
            views.setOnClickPendingIntent(R.id.info, open)
            return views
        }

        private fun actionIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, SpotifyWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
        }

        private fun openIntent(context: Context, hasAccess: Boolean): PendingIntent {
            val intent = if (hasAccess) {
                context.packageManager.getLaunchIntentForPackage(SpotifyController.PACKAGE)
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } ?: Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
