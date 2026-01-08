package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import com.example.keyboard.R
import keyboard.Keyboard as GoKeyboard

class MyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    // 1. Declare at class level (Top of the class)
    private var keyboardView: KeyboardView? = null

    override fun onCreateInputView(): View {
        // 2. Assign the view to the class-level variable
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboardView?.keyboard = Keyboard(this, R.xml.qwerty)
        keyboardView?.setOnKeyboardActionListener(this)
        return keyboardView!!
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            -5 -> ic.deleteSurroundingText(1, 0)
            -1 -> {
                // 3. Now this reference will work!
                keyboardView?.let {
                    it.isShifted = !it.isShifted
                    it.invalidateAllKeys()
                }
            }
            -4 -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            32 -> ic.commitText(" ", 1)
            else -> {
                val isShifted = keyboardView?.isShifted ?: false
                val result = GoKeyboard.processKey(primaryCode.toLong(), isShifted)
                ic.commitText(result, 1)

                if (isShifted) {
                    keyboardView?.isShifted = false
                    keyboardView?.invalidateAllKeys()
                }
            }
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
