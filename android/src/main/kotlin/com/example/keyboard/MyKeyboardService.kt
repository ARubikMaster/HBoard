package com.example.keyboard

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard as AndroidKeyboard  // ALIAS THIS
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import keyboard.Keyboard as GoLib // ALIAS YOUR GO LIBRARY

class MyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var qwertyKeyboard: AndroidKeyboard? = null
    private var symbolsKeyboard: AndroidKeyboard? = null
    private var mathKeyboard: AndroidKeyboard? = null
    
    private lateinit var clipboardBar: LinearLayout
    private lateinit var clipboardPreview: TextView
    private lateinit var clipboardManager: ClipboardManager
    
    private var isSymbols = false

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)

        // Initialize Clipboard components
        clipboardBar = root.findViewById(R.id.clipboard_bar)
        clipboardPreview = root.findViewById(R.id.clipboard_preview)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val btnPaste: Button = root.findViewById(R.id.btn_paste)
        val btnClear: Button = root.findViewById(R.id.btn_clear)

        btnPaste.setOnClickListener {
            val clip = clipboardManager.primaryClip?.getItemAt(0)?.text
            if (clip != null) {
                currentInputConnection?.commitText(clip, 1)
            }
        }

        btnClear.setOnClickListener {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
            updateClipboardBar()
        }

        // Initialize KeyboardView
        keyboardView = root.findViewById(R.id.keyboard_view)
        qwertyKeyboard = AndroidKeyboard(this, R.xml.qwerty)
        symbolsKeyboard = AndroidKeyboard(this, R.xml.symbols)
        mathKeyboard = AndroidKeyboard(this, R.xml.mathsymbols)

        keyboardView?.keyboard = qwertyKeyboard
        keyboardView?.setOnKeyboardActionListener(this)

        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateClipboardBar()
    }

    private fun updateClipboardBar() {
        val clipData = clipboardManager.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text
            if (!text.isNullOrBlank()) {
                clipboardPreview.text = text
                clipboardBar.visibility = View.VISIBLE
                return
            }
        }
        clipboardBar.visibility = View.GONE
    }

    // --- Keyboard Logic ---

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        
        when (primaryCode) {
	    -1 -> { // Shift Key
		keyboardView?.let {
		    // Toggle the shifted state
		    val currentShiftState = it.isShifted
		    it.isShifted = !currentShiftState
        
		    // Force the keyboard to redraw the labels (e.g., lowercase to uppercase)
		    it.invalidateAllKeys()
		}
	    }
            -2 -> keyboardView?.keyboard = symbolsKeyboard
            -67 -> keyboardView?.keyboard = symbolsKeyboard
            -68 -> keyboardView?.keyboard = qwertyKeyboard
            -69 -> keyboardView?.keyboard = mathKeyboard
            -5 -> ic.deleteSurroundingText(1, 0)
            -4 -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            32 -> ic.commitText(" ", 1)
            else -> {
                val isShifted = keyboardView?.isShifted ?: false
                val result = GoLib.processKey(primaryCode.toLong(), isShifted)
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
