package com.example.keyboard

import android.content.Context
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.View

class LatinKeyboardView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {

    init {
        isPreviewEnabled = false
    }

    override fun setPopupParent(v: View) {
        // This ensures the popup window is anchored to the main keyboard view
        // so the 'glide' motion stays within the same touch coordinate system.
        super.setPopupParent(this)
    }

    override fun onLongPress(key: android.inputmethodservice.Keyboard.Key?): Boolean {
        // This confirms the long press is happening. 
        // If the popup doesn't appear, it's usually because popupResId 
        // was missing in the XML.
        return super.onLongPress(key)
    }
}
