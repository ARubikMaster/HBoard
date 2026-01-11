package com.example.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class LatinKeyboardView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {

    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()

    init {
        isPreviewEnabled = false
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the base keyboard (labels and backgrounds)
        super.onDraw(canvas)
        
        val keyboard = keyboard ?: return
        val typedValue = TypedValue()

        // Resolve Dynamic Colors from Theme
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
        val colorOnPrimary = typedValue.data
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val colorOnSurface = typedValue.data

        for (key in keyboard.keys) {
            // --- ENTER KEY CUSTOM RENDERING ---
            if (key.codes.contains(-4)) {
                val rect = RectF(
                    key.x.toFloat(), 
                    key.y.toFloat(), 
                    (key.x + key.width).toFloat(), 
                    (key.y + key.height).toFloat()
                )

                val radius = key.height * 0.15f 

                canvas.save()
                clipPath.reset()
                clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
                canvas.clipPath(clipPath) 
                
                accentPaint.color = colorPrimary
                canvas.drawRect(rect, accentPaint) 
                
                canvas.restore()

                // Draw Enter Icon
                drawKeyIcon(canvas, key, colorOnPrimary)
            }

            // --- FUNCTIONAL ICONS (SHIFT, BACKSPACE, SETTINGS) ---
            // Added -10 here to ensure the Settings icon is actually drawn
            if (key.codes.contains(-1) || key.codes.contains(-5) || key.codes.contains(-10)) {
                drawKeyIcon(canvas, key, colorOnSurface)
            }
        }
    }

    private fun drawKeyIcon(canvas: Canvas, key: android.inputmethodservice.Keyboard.Key, color: Int) {
        key.icon?.let { icon ->
            icon.mutate()
            icon.setTint(color)

            // Calculate centered bounds (40% of key height is the standard icon size)
            val iconSize = (key.height * 0.40).toInt()
            val left = key.x + (key.width - iconSize) / 2
            val top = key.y + (key.height - iconSize) / 2
            
            icon.setBounds(left, top, left + iconSize, top + iconSize)
            icon.draw(canvas)
        }
    }

    override fun setPopupParent(v: View) {
        super.setPopupParent(this)
    }
}
