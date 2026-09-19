package com.geostamp.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * Utility object for clipboard operations.
 */
object ClipboardUtil {

    /**
     * Copies the given text to the system clipboard and shows a confirmation toast.
     *
     * @param context The Android context used to access system services.
     * @param label A user-visible label for the clip data.
     * @param text The text content to copy to the clipboard.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
