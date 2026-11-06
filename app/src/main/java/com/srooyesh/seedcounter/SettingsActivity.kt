package com.srooyesh.seedcounter

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt

class SettingsActivity : AppCompatActivity() {
    private val settingsStore by lazy { SettingsStore(this) }
    private var settings = AppSettings()

    private lateinit var scanMode: Spinner
    private lateinit var recognitionMode: Spinner
    private lateinit var numberScanMode: Spinner
    private lateinit var scanQuality: Spinner
    private lateinit var scanWindow: Spinner
    private lateinit var autoSaveDelay: Spinner // 👈 اضافه شد
    private lateinit var stableReads: Spinner
    private lateinit var minDigits: NumberPicker
    private lateinit var maxDigits: NumberPicker
    private lateinit var defaultFlash: Spinner
    private lateinit var autoFocus: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var duplicateCheck: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var haptic: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var sound: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var voiceGuidance: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var frameWidth: Slider
    private lateinit var frameHeight: Slider
    private lateinit var frameWidthValue: android.widget.TextView
    private lateinit var frameHeightValue: android.widget.TextView
    private lateinit var dateOutput: RadioGroup
    private lateinit var outRow: CheckBox
    private lateinit var outVariety: CheckBox
    private lateinit var outCustomer: CheckBox
    private lateinit var outNumber: CheckBox
    private lateinit var outBarcode: CheckBox
    private lateinit var outTime: CheckBox
    private lateinit var outSession: CheckBox

    private val openBackup = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) restoreBackup(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<android.widget.TextView>(R.id.settingsFooterVersion).text =
            getString(R.string.settings_footer, BuildConfig.VERSION_NAME)
        settings = settingsStore.get()

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.settingsToolbar).setNavigationOnClickListener { finish() }

        scanMode = findViewById(R.id.scanMode)
        recognitionMode = findViewById(R.id.recognitionMode)
        numberScanMode = findViewById(R.id.numberScanMode)
        scanQuality = findViewById(R.id.scanQuality)
        scanWindow = findViewById(R.id.scanWindow)
        autoSaveDelay = findViewById(R.id.autoSaveDelay) // 👈 اضافه شد
        stableReads = findViewById(R.id.stableReads)
        minDigits = findViewById(R.id.minDigits)
        maxDigits = findViewById(R.id.maxDigits)
        defaultFlash = findViewById(R.id.defaultFlash)
        autoFocus = findViewById(R.id.autoFocus)
        duplicateCheck = findViewById(R.id.duplicateCheck)
        haptic = findViewById(R.id.haptic)
        sound = findViewById(R.id.sound)
        voiceGuidance = findViewById(R.id.voiceGuidance)
        frameWidth = findViewById(R.id.frameWidth)
        frameHeight = findViewById(R.id.frameHeight)
        frameWidthValue = findViewById(R.id.frameWidthValue)
        frameHeightValue = findViewById(R.id.frameHeightValue)
        dateOutput = findViewById(R.id.dateOutput)
        outRow = findViewById(R.id.outRow)
        outVariety = findViewById(R.id.outVariety)
        outCustomer = findViewById(R.id.outCustomer)
        outNumber = findViewById(R.id.outNumber)
        outBarcode = findViewById(R.id.outBarcode)
        outTime = findViewById(R.id.outTime)
        outSession = findViewById(R.id.outSession)

        configureSpinners()
        configureNumberPickers()
        configureFrameSliders()
        populateFromSettings(settings)

        findViewById<Button>(R.id.backupNow).setOnClickListener {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { BackupExporter(this@SettingsActivity).create() }
                if (result.isSuccess) startActivity(result.getOrThrow())
                else toast(result.exceptionOrNull()?.message ?: getString(R.string.backup_error))
            }
        }
        findViewById<Button>(R.id.restoreBackup).setOnClickListener {
            openBackup.launch(arrayOf("application/json", "text/json", "text/plain"))
        }
        findViewById<Button>(R.id.resetSettings).setOnClickListener {
            settings = AppSettings()
            settingsStore.save(settings)
            populateFromSettings(settingsStore.get())
            toast(getString(R.string.settings_reset_saved))
        }
        findViewById<Button>(R.id.resetFrame).setOnClickListener {
            frameWidth.value = 84f
            frameHeight.value = 28f
            frameWidthValue.text = getString(R.string.frame_width_value, 84)
            frameHeightValue.text = getString(R.string.frame_height_value, 28)
            toast(getString(R.string.frame_reset))
        }
        findViewById<Button>(R.id.saveSettings).setOnClickListener { saveAndClose() }
    }

    private fun configureSpinners() {
        setSpinner(scanMode, arrayOf(getString(R.string.scan_mode_manual), getString(R.string.scan_mode_auto)), 0)
        setSpinner(recognitionMode, arrayOf(getString(R.string.recognition_number), getString(R.string.recognition_barcode), getString(R.string.recognition_both)), 0)
        setSpinner(numberScanMode, arrayOf(getString(R.string.number_scan_simple), getString(R.string.number_scan_smart)), 1)
        setSpinner(scanQuality, arrayOf(getString(R.string.quality_standard), getString(R.string.quality_precise), getString(R.string.quality_max)), 2)
        setSpinner(defaultFlash, arrayOf(getString(R.string.flash_off), getString(R.string.flash_on)), 0)

        val windowSeconds = intArrayOf(3, 5, 8, 10)
        setSpinner(scanWindow, windowSeconds.map { getString(R.string.scan_window_value, it) }.toTypedArray(), 1)

        // 👇 تنظیم منوی زمان ثبت خودکار
        val autoSaveSeconds = intArrayOf(1, 2, 3, 4, 5, 6, 8, 10)
        setSpinner(autoSaveDelay, autoSaveSeconds.map { "$it ثانیه" }.toTypedArray(), 2)

        val stableOptions = intArrayOf(2, 3, 4, 5)
        setSpinner(stableReads, stableOptions.map { getString(R.string.stable_reads_value, it) }.toTypedArray(), 1)
    }

    private fun configureNumberPickers() {
        minDigits.minValue = 2
        minDigits.maxValue = 16
        maxDigits.minValue = 2
        maxDigits.maxValue = 16
        minDigits.setOnValueChangedListener { _, _, newValue ->
            if (maxDigits.value < newValue) maxDigits.value = newValue
        }
        maxDigits.setOnValueChangedListener { _, _, newValue ->
            if (minDigits.value > newValue) minDigits.value = newValue
        }
    }

    private fun configureFrameSliders() {
        frameWidth.valueFrom = 56f
        frameWidth.valueTo = 96f
        frameWidth.stepSize = 1f
        frameHeight.valueFrom = 12f
        frameHeight.valueTo = 58f
        frameHeight.stepSize = 1f

        frameWidth.addOnChangeListener { _, value, _ ->
            frameWidthValue.text = getString(R.string.frame_width_value, value.roundToInt())
        }
        frameHeight.addOnChangeListener { _, value, _ ->
            frameHeightValue.text = getString(R.string.frame_height_value, value.roundToInt())
        }
    }

    private fun populateFromSettings(s: AppSettings) {
        settings = s.sanitized()
        scanMode.setSelection(if (settings.scanMode == ScanMode.AUTO) 1 else 0)
        recognitionMode.setSelection(
            when (settings.recognitionMode) {
                RecognitionMode.NUMBER -> 0
                RecognitionMode.BARCODE -> 1
                RecognitionMode.BOTH -> 2
            }
        )
        numberScanMode.setSelection(if (settings.numberScanMode == NumberScanMode.SMART) 1 else 0)
        scanQuality.setSelection(settings.scanQuality)
        defaultFlash.setSelection(if (settings.flashMode == 1) 1 else 0)

        val windows = intArrayOf(3, 5, 8, 10)
        scanWindow.setSelection(windows.indexOf(settings.manualScanWindowSec).coerceAtLeast(0))
        
        // 👇 نمایش زمان ثبت خودکار ذخیره شده
        val autoSaveSeconds = intArrayOf(1, 2, 3, 4, 5, 6, 8, 10)
        autoSaveDelay.setSelection(autoSaveSeconds.indexOf(settings.autoSaveDelaySec).coerceAtLeast(0))

        val stable = intArrayOf(2, 3, 4, 5)
        stableReads.setSelection(stable.indexOf(settings.stableReads).coerceAtLeast(0))

        minDigits.value = settings.minDigits
        maxDigits.value = settings.maxDigits
        autoFocus.isChecked = settings.autoFocus
        duplicateCheck.isChecked = settings.duplicateCheck
        haptic.isChecked = settings.haptic
        sound.isChecked = settings.sound
        voiceGuidance.isChecked = settings.voiceGuidance

        frameWidth.value = settings.scanFrameWidthPct.toFloat()
        frameHeight.value = settings.scanFrameHeightPct.toFloat()
        frameWidthValue.text = getString(R.string.frame_width_value, settings.scanFrameWidthPct)
        frameHeightValue.text = getString(R.string.frame_height_value, settings.scanFrameHeightPct)

        when (settings.dateOutput) {
            "jalali" -> dateOutput.check(R.id.dateJalali)
            "gregorian" -> dateOutput.check(R.id.dateGregorian)
            else -> dateOutput.check(R.id.dateBoth)
        }
        outRow.isChecked = settings.outRow
        outVariety.isChecked = settings.outVariety
        outCustomer.isChecked = settings.outCustomer
        outNumber.isChecked = settings.outNumber
        outBarcode.isChecked = settings.outBarcode
        outTime.isChecked = settings.outTime
        outSession.isChecked = settings.outSession
    }

    private fun saveAndClose() {
        val dateMode = when (dateOutput.checkedRadioButtonId) {
            R.id.dateJalali -> "jalali"
            R.id.dateGregorian -> "gregorian"
            else -> "both"
        }
        val windows