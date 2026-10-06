package com.srooyesh.seedcounter

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    companion object {
        const val PREFS = "seed_data_v2"
        const val KEY_ACTIVE_VARIETY = "active_variety"
        const val KEY_ACTIVE_DATE = "active_date"
        const val PREFIX = "record|"
        const val ACTION_QUICK_SCAN = "com.srooyesh.seedcounter.action.QUICK_SCAN"
    }

    private val repo by lazy { SeedRepository(this) }
    private var activeSession: SeedSession? = null
    private val displayRows = mutableListOf<HistoryRow>()
    private lateinit var adapter: HistoryAdapter
    private var refreshJob: Job? = null

    private lateinit var today: TextView
    private lateinit var status: TextView
    private lateinit var active: TextView
    private lateinit var count: TextView
    private lateinit var historySummary: TextView
    private lateinit var historySearch: EditText
    private lateinit var historySearchCount: TextView
    private lateinit var variety: EditText
    private lateinit var customer: EditText
    private lateinit var manual: EditText

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshScreen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<android.widget.TextView>(R.id.footerVersion).text =
            getString(R.string.footer_version, BuildConfig.VERSION_NAME)

        today = findViewById(R.id.today)
        status = findViewById(R.id.status)
        active = findViewById(R.id.active)
        count = findViewById(R.id.count)
        historySummary = findViewById(R.id.historySummary)
        historySearch = findViewById(R.id.historySearch)
        historySearchCount = findViewById(R.id.historySearchCount)
        variety = findViewById(R.id.variety)
        customer = findViewById(R.id.customer)
        manual = findViewById(R.id.manual)

        val list = findViewById<RecyclerView>(R.id.list)
        adapter = HistoryAdapter(displayRows)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<Button>(R.id.start).setOnClickListener { startSession() }
        findViewById<Button>(R.id.add).setOnClickListener {
            addManualNumber(manual.text.toString())
            manual.setText("")
        }
        manual.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                addManualNumber(manual.text.toString())
                manual.setText("")
                true
            } else false
        }
        findViewById<Button>(R.id.scan).setOnClickListener { openCamera() }
        findViewById<Button>(R.id.finish).setOnClickListener { finishSession() }
        findViewById<Button>(R.id.previewExcel).setOnClickListener { openExcelPreview() }
        findViewById<Button>(R.id.export).setOnClickListener { exportExcel() }
        findViewById<Button>(R.id.backup).setOnClickListener { backupJson() }
        findViewById<View>(R.id.settings).setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.delete).setOnClickListener { confirmDeleteAll() }
        historySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshScreen() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        historySearch.setOnEditorActionListener { _, _, _ -> true }

        refreshScreen()
        handleQuickScanIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuickScanIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshScreen()
    }

    private fun handleQuickScanIntent(intent: Intent?) {
        if (intent?.action != ACTION_QUICK_SCAN) return
        intent.action = null
        lifecycleScope.launch {
            val session = withContext(Dispatchers.IO) { repo.getActiveSession() }
            if (session == null) {
                toast(getString(R.string.quick_scan_no_active_session))
                return@launch
            }
            activeSession = session
            startActivity(Intent(this@MainActivity, CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_SESSION_ID, session.id)
            })
        }
    }

    private fun refreshScreen() {
        today.text = getString(R.string.today_prefix, PersianDate.today())
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            val session = withContext(Dispatchers.IO) { repo.getActiveSession() }
            val todaySummary = withContext(Dispatchers.IO) { repo.getTodaySummary() }
            historySummary.text = getString(R.string.history_summary, todaySummary.first, todaySummary.second)
            activeSession = session
            val query = historySearch.text.toString().trim()

            if (session != null) {
                variety.setText(session.variety)
                customer.setText(session.customer)
                active.text = getString(
                    R.string.active_session_label,
                    session.variety,
                    if (session.customer.isBlank()) getString(R.string.no_customer) else session.customer
                )
                val currentCount = withContext(Dispatchers.IO) { repo.getRecordCount(session.id) }
                count.text = getString(R.string.active_count_simple, currentCount)
                status.text = getString(R.string.status_active)
                findViewById<Button>(R.id.start).isEnabled = false
                findViewById<Button>(R.id.finish).isEnabled = true
                findViewById<Button>(R.id.scan).isEnabled = true
                findViewById<Button>(R.id.add).isEnabled = true

                if (query.isBlank()) {
                    val loaded = withContext(Dispatchers.IO) { repo.getRecords(session.id) }
                    displayRows.clear()
                    displayRows.addAll(
                        loaded.map { record ->
                            HistoryRow(
                                id = record.id,
                                sessionId = record.sessionId,
                                variety = session.variety,
                                customer = session.customer,
                                number = record.number,
                                barcode = record.barcode,
                                dateGregorian = record.dateGregorian,
                                dateJalali = record.dateJalali,
                                time = record.time
                            )
                        }
                    )
                    historySearchCount.text = getString(R.string.history_current_count, loaded.size)
                } else {
                    showSearchResults(query)
                }
            } else {
                active.text = getString(R.string.no_active_variety)
                count.text = getString(R.string.active_count_simple, 0)
                status.text = getString(R.string.status_initial)
                findViewById<Button>(R.id.start).isEnabled = true
                findViewById<Button>(R.id.finish).isEnabled = false
                findViewById<Button>(R.id.scan).isEnabled = false
                findViewById<Button>(R.id.add).isEnabled = false
                if (query.isBlank()) {
                    displayRows.clear()
                    historySearchCount.text = getString(R.string.history_current_count, 0)
                } else {
                    showSearchResults(query)
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private suspend fun showSearchResults(query: String) {
        val found = withContext(Dispatchers.IO) { repo.searchHistory(query) }
        displayRows.clear()
        displayRows.addAll(found)
        historySearchCount.text = getString(R.string.history_search_count, found.size)
        status.text = getString(R.string.history_search_status, found.size)
    }

    private fun startSession() {
        val v = variety.text.toString().trim().replace("|", " ").replace("\n", " ").replace("\r", " ")
        val c = customer.text.toString().trim().replace("|", " ").replace("\n", " ").replace("\r", " ")
        if (v.isBlank()) {
            toast(getString(R.string.variety_required))
            return
        }
        if (v.length > 100 || c.length > 120) {
            toast(getString(R.string.text_too_long))
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { repo.startSession(v, c) }
            if (result.isFailure) {
                toast(result.exceptionOrNull()?.message ?: getString(R.string.active_session_exists))
                return@launch
            }
            refreshScreen()
            toast(getString(R.string.session_started, v))
        }
    }

    private fun openCamera() {
        lifecycleScope.launch {
            val session = withContext(Dispatchers.IO) { repo.getActiveSession() } ?: run {
                toast(getString(R.string.start_variety_required))
                return@launch
            }
            activeSession = session
            startActivity(Intent(this@MainActivity, CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_SESSION_ID, session.id)
            })
        }
    }

    private fun addManualNumber(raw: String) {
        val session = activeSession ?: run {
            toast(getString(R.string.start_variety_required))
            return
        }
        val number = Storage.normalizePacketNumber(raw)
        val s = SettingsStore(this).get()
        if (number.length !in s.minDigits..s.maxDigits) {
            toast(getString(R.string.invalid_packet_number_range, s.minDigits, s.maxDigits))
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.addRecord(session.id, number, "", SettingsStore(this@MainActivity).get().duplicateCheck)
            }
            if (result.isFailure) {
                toast(result.exceptionOrNull()?.message ?: getString(R.string.save_error))
                return@launch
            }
            refreshScreen()
        }
    }

    private fun finishSession() {
        val session = activeSession ?: run {
            toast(getString(R.string.finish_none))
            return
        }
        lifecycleScope.launch {
            val total = withContext(Dispatchers.IO) { repo.getRecords(session.id).size }
            AlertDialog.Builder(this@MainActivity)
                .setTitle(getString(R.string.finish_title))
                .setMessage(
                    getString(
                        R.string.finish_message_with_customer,
                        session.variety,
                        session.customer.ifBlank { getString(R.string.no_customer) },
                        total
                    )
                )
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.finish_variety)) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { repo.finishSession(session.id) }
                        refreshScreen()
                        toast(getString(R.string.finish_done))
                    }
                }
                .show()
        }
    }

    private fun openExcelPreview() {
        lifecycleScope.launch {
            val hasRows = withContext(Dispatchers.IO) { repo.getAllRows().isNotEmpty() }
            if (!hasRows) {
                toast(getString(R.string.excel_empty))
                return@launch
            }
            startActivity(Intent(this@MainActivity, ExportPreviewActivity::class.java))
        }
    }

    private fun exportExcel() {
        lifecycleScope.launch {
            status.text = getString(R.string.exporting_excel)
            val result = withContext(Dispatchers.IO) { ExcelExporter(this@MainActivity).export() }
            if (result.isSuccess) {
                startActivity(result.getOrThrow())
                status.text = getString(R.string.excel_ready)
            } else {
                status.text = getString(R.string.excel_error)
                toast(result.exceptionOrNull()?.message ?: getString(R.string.excel_error))
            }
        }
    }

    private fun backupJson() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { BackupExporter(this@MainActivity).create() }
            if (result.isSuccess) startActivity(result.getOrThrow())
            else toast(result.exceptionOrNull()?.message ?: getString(R.string.backup_error))
        }
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_title))
            .setMessage(getString(R.string.delete_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete_confirm)) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { repo.deleteAll() }
                    refreshScreen()
                    toast(getString(R.string.delete_done))
                }
            }
            .show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
