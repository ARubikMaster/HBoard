package com.example.keyboard

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard as AndroidKeyboard
import android.inputmethodservice.KeyboardView
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import keyboard.Keyboard as GoLib

class MyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: LatinKeyboardView? = null
    private var qwertyKeyboard: AndroidKeyboard? = null
    private var symbolsKeyboard: AndroidKeyboard? = null
    private var mathKeyboard: AndroidKeyboard? = null
    
    private lateinit var clipboardBar: LinearLayout
    private lateinit var settingsPanel: LinearLayout
    private lateinit var clipboardPreview: TextView
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var prefs: SharedPreferences
    private var vibrator: Vibrator? = null

    // Shift state tracking: 0=Off, 1=Shift, 2=Caps Lock
    private var shiftState = 0 
    private var lastShiftTime: Long = 0

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)
        prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        loadKeyboardLayouts()

        keyboardView = root.findViewById<View>(R.id.keyboard_view) as? LatinKeyboardView
        keyboardView?.apply {
            setPopupParent(this)
            setPopupOffset(0, -100) 
            isPreviewEnabled = false 
            keyboard = qwertyKeyboard
            setOnKeyboardActionListener(this@MyKeyboardService)
        }

        clipboardBar = root.findViewById(R.id.clipboard_bar)
        settingsPanel = root.findViewById(R.id.settings_panel)
        clipboardPreview = root.findViewById(R.id.clipboard_preview)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        setupSettingsPanel(root)
        updateClipboardBar()

        // Clipboard Listeners
        root.findViewById<Button>(R.id.btn_paste).setOnClickListener {
            val clip = clipboardManager.primaryClip?.getItemAt(0)?.text
            if (!clip.isNullOrEmpty()) {
                currentInputConnection?.commitText(clip, 1)
            }
        }

        root.findViewById<Button>(R.id.btn_clear).setOnClickListener {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
            updateClipboardBar()
        }

        return root
    }

    private fun loadKeyboardLayouts() {
        val isOnRight = prefs.getBoolean("thorn_on_right", false)
        qwertyKeyboard = if (isOnRight) {
            AndroidKeyboard(this, R.xml.qwerty_right)
        } else {
            AndroidKeyboard(this, R.xml.qwerty_left)
        }
        
        updateKeyLabels()

        symbolsKeyboard = AndroidKeyboard(this, R.xml.symbols)
        mathKeyboard = AndroidKeyboard(this, R.xml.mathsymbols)
    }

    private fun updateKeyLabels() {
        val isShifted = shiftState > 0
        val isEth = prefs.getBoolean("use_eth_instead", false)
        
        val thornLabel = if (isEth) {
            if (isShifted) "Ð" else "ð"
        } else {
            if (isShifted) "Þ" else "þ"
        }
        
        qwertyKeyboard?.keys?.forEach { key ->
            if (key.codes.contains(254)) {
                key.label = thornLabel
            }
            
            if (key.codes.contains(-1)) {
                key.icon = when (shiftState) {
                    2 -> resources.getDrawable(R.drawable.ic_shift_caps, null)
                    1 -> resources.getDrawable(R.drawable.ic_shift_active, null)
                    else -> resources.getDrawable(R.drawable.ic_shift, null)
                }
            }
        }
        keyboardView?.invalidateAllKeys()
    }

    private fun setupSettingsPanel(root: View) {
        val thornSwitch: Switch = root.findViewById(R.id.switch_thorn_side)
        val thornGroup: RadioGroup = root.findViewById(R.id.radio_thorn_type)
        val vibrateSwitch: Switch = root.findViewById(R.id.switch_vibrate)

        thornSwitch.isChecked = prefs.getBoolean("thorn_on_right", false)
        val isEth = prefs.getBoolean("use_eth_instead", false)
        thornGroup.check(if (isEth) R.id.radio_th else R.id.radio_þ)
        vibrateSwitch.isChecked = prefs.getBoolean("vibrate_on_keypress", false)

        thornSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("thorn_on_right", isChecked).apply()
            loadKeyboardLayouts()
            keyboardView?.keyboard = qwertyKeyboard
        }

        thornGroup.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit().putBoolean("use_eth_instead", checkedId == R.id.radio_th).apply()
            updateKeyLabels()
        }

        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibrate_on_keypress", isChecked).apply()
        }

        root.findViewById<Button>(R.id.btn_close_settings).setOnClickListener { toggleSettings() }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateClipboardBar()
        loadKeyboardLayouts()
        keyboardView?.keyboard = qwertyKeyboard
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

    fun toggleSettings() {
        if (settingsPanel.visibility == View.VISIBLE) {
            settingsPanel.visibility = View.GONE
            keyboardView?.visibility = View.VISIBLE
        } else {
            settingsPanel.visibility = View.VISIBLE
            keyboardView?.visibility = View.GONE
        }
    }

    private fun doVibrate() {
        if (prefs.getBoolean("vibrate_on_keypress", false)) {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, 200))
        }
    }

    override fun onPress(primaryCode: Int) {
        doVibrate()
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        
        when (primaryCode) {
            -1 -> {
                val now = System.currentTimeMillis()
                if (now - lastShiftTime < 300) {
                    shiftState = 2
                } else {
                    shiftState = if (shiftState == 0) 1 else 0
                }
                lastShiftTime = now
                keyboardView?.isShifted = (shiftState > 0)
                updateKeyLabels()
            }
            -2, -67 -> keyboardView?.keyboard = symbolsKeyboard
            -68 -> {
                loadKeyboardLayouts()
                keyboardView?.keyboard = qwertyKeyboard
            }
            -69 -> keyboardView?.keyboard = mathKeyboard
            -10 -> toggleSettings() 
            -5 -> ic.deleteSurroundingText(1, 0)
            -4 -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            32 -> ic.commitText(" ", 1)
            else -> {
                val isShifted = shiftState > 0
                val result = if (primaryCode == 254 && prefs.getBoolean("use_eth_instead", false)) {
                    if (isShifted) "Ð" else "ð"
                } else {
                    GoLib.processKey(primaryCode.toLong(), isShifted)
                }
                
                ic.commitText(result, 1)

                if (shiftState == 1) {
                    shiftState = 0
                    keyboardView?.isShifted = false
                    updateKeyLabels()
                }
            }
        }
    }

    override fun onRelease(p0: Int) {}
    override fun onText(p0: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
