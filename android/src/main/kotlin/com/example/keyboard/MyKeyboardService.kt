package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import com.example.keyboard.R
import keyboard.Keyboard as GoKeyboard

class MyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var qwertyKeyboard: Keyboard? = null
    private var symbolsKeyboard: Keyboard? = null
    private var mathKeyboard: Keyboard? = null
    private var status = "qwerty"

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        
        // Initialize all layouts
        qwertyKeyboard = Keyboard(this, R.xml.qwerty)
        symbolsKeyboard = Keyboard(this, R.xml.symbols)
	mathKeyboard = Keyboard(this, R.xml.mathsymbols)
        
        keyboardView?.keyboard = qwertyKeyboard
        keyboardView?.setOnKeyboardActionListener(this)
        return keyboardView!!
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            -67 -> {
                status = "symbols"
                keyboardView?.keyboard = symbolsKeyboard 
            }

            -68 -> {
                status = "qwerty"
                keyboardView?.keyboard = qwertyKeyboard
            }

            -69 -> {
                status = "math"
                keyboardView?.keyboard = mathKeyboard
            }

            -5 -> ic.deleteSurroundingText(1, 0)
            -1 -> { // Shift (only relevant for QWERTY)
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
                
                // Optional: return to QWERTY after typing a symbol
                /*
                if (isSymbols) {
                    isSymbols = false
                    keyboardView?.keyboard = qwertyKeyboard
                }
                */
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
