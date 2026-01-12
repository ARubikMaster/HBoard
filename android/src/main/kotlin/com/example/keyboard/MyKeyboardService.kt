package com.example.keyboard

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard as AndroidKeyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
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

    private var shiftState = 0 
    private var lastShiftTime: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeleting) {
                currentInputConnection?.deleteSurroundingText(1, 0)
                handler.postDelayed(this, 50) 
            }
        }
    }

    override fun onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this.application)
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener { updateClipboardBar() }
    }

    override fun onCreateInputView(): View {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_MyKeyboard)
        val dynamicContext = DynamicColors.wrapContextIfAvailable(themedContext)
        val themedInflater = layoutInflater.cloneInContext(dynamicContext)
        val root = themedInflater.inflate(R.layout.keyboard_view, null)
        
        prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        keyboardView = root.findViewById(R.id.keyboard_view)
        keyboardView?.setOnKeyboardActionListener(this)

        clipboardBar = root.findViewById(R.id.clipboard_bar)
        clipboardPreview = root.findViewById(R.id.clipboard_preview)
        
        root.findViewById<Button>(R.id.btn_paste)?.setOnClickListener {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                currentInputConnection?.commitText(clipData.getItemAt(0).text ?: "", 1)
            }
        }

        root.findViewById<Button>(R.id.btn_clear)?.setOnClickListener {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
            updateClipboardBar()
        }

        settingsPanel = root.findViewById(R.id.settings_panel)
        setupSettingsPanel(root)
        
        loadKeyboardLayouts()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadKeyboardLayouts()
        updateClipboardBar()
    }

    private fun loadKeyboardLayouts() {
        val isOnRight = prefs.getBoolean("thorn_on_right", false)
        val useEth = prefs.getBoolean("use_eth_instead", false)

        // Select the specific XML resource based on preferences
        val layoutRes = when {
            isOnRight && useEth -> R.xml.qwerty_right_eth
            isOnRight && !useEth -> R.xml.qwerty_right_thorn
            !isOnRight && useEth -> R.xml.qwerty_left_eth
            else -> R.xml.qwerty_left_thorn
        }

        qwertyKeyboard = AndroidKeyboard(this, layoutRes)
        symbolsKeyboard = AndroidKeyboard(this, R.xml.symbols)
        mathKeyboard = AndroidKeyboard(this, R.xml.mathsymbols)
        
        keyboardView?.keyboard = qwertyKeyboard
        updateKeyLabels()
    }

    private fun updateKeyLabels() {
        val isShifted = shiftState > 0
        keyboardView?.isShifted = isShifted

        val allKeyboards = listOf(qwertyKeyboard, symbolsKeyboard, mathKeyboard)
        allKeyboards.forEach { kb ->
            kb?.keys?.forEach { key ->
                // Update Shift icons specifically based on state
                if (key.codes[0] == -1) {
                    val iconRes = when (shiftState) {
                        2 -> R.drawable.ic_shift_caps
                        1 -> R.drawable.ic_shift_active
                        else -> R.drawable.ic_shift
                    }
                    key.icon = ContextCompat.getDrawable(this, iconRes)
                }
            }
        }
        keyboardView?.invalidateAllKeys()
    }

    private fun setupSettingsPanel(root: View) {
        root.findViewById<Switch>(R.id.switch_thorn_side).apply {
            isChecked = prefs.getBoolean("thorn_on_right", false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("thorn_on_right", isChecked).apply()
                loadKeyboardLayouts()
            }
        }

        root.findViewById<RadioGroup>(R.id.radio_thorn_type).apply {
            check(if (prefs.getBoolean("use_eth_instead", false)) R.id.radio_th else R.id.radio_þ)
            setOnCheckedChangeListener { _, checkedId ->
                prefs.edit().putBoolean("use_eth_instead", checkedId == R.id.radio_th).apply()
                // Re-load the full layout to update the key codes/popups correctly
                loadKeyboardLayouts()
            }
        }

        root.findViewById<Switch>(R.id.switch_vibrate).apply {
            isChecked = prefs.getBoolean("vibrate_on_keypress", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("vibrate_on_keypress", isChecked).apply()
            }
        }

        root.findViewById<Button>(R.id.btn_close_settings).setOnClickListener { toggleSettings() }
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

    override fun onPress(primaryCode: Int) { 
        if (prefs.getBoolean("vibrate_on_keypress", true)) {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        if (primaryCode == -5) {
            isDeleting = true
            handler.postDelayed(deleteRunnable, 400)
        }
    }

    override fun onRelease(primaryCode: Int) {
        if (primaryCode == -5) {
            isDeleting = false
            handler.removeCallbacks(deleteRunnable)
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            -1 -> {
                val now = System.currentTimeMillis()
                shiftState = if (now - lastShiftTime < 300) 2 else (if (shiftState == 0) 1 else 0)
                lastShiftTime = now
                updateKeyLabels()
            }
            -2, -67 -> {
                keyboardView?.keyboard = symbolsKeyboard
                updateKeyLabels()
            }
            -68 -> { 
                keyboardView?.keyboard = qwertyKeyboard
                updateKeyLabels()
            }
            -69 -> {
                keyboardView?.keyboard = mathKeyboard
                updateKeyLabels()
            }
            -10 -> toggleSettings() 
            -5 -> ic.deleteSurroundingText(1, 0)
            -4 -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            32 -> ic.commitText(" ", 1)
            else -> {
                // Character processing via the Go library
                val res = GoLib.processKey(primaryCode.toLong(), shiftState > 0)
                ic.commitText(res, 1)
                
                if (shiftState == 1) {
                    shiftState = 0
                    updateKeyLabels()
                }
            }
        }
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

    override fun onText(text: CharSequence?) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        if (shiftState == 1) {
            shiftState = 0
            updateKeyLabels()
        }
    }

    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
