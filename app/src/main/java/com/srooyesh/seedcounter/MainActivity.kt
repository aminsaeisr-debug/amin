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
            
            // 👇 فقط این بخش تغییر کرد
            if (displayRows.isEmpty()) {
                adapter.notifyItemRangeRemoved(0, adapter.itemCount)
            } else {
                adapter.notifyItemRangeChanged(0, displayRows.size)
            }
        }
    }