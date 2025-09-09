package dev.mooner.eevee.view.log

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.mooner.eevee.Constants
import dev.mooner.eevee.databinding.ActivityLogViewBinding
import dev.mooner.eevee.utils.LogUtils
import dev.mooner.eevee.view.settings.SettingsRepository

class LogViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogViewBinding
    private lateinit var logAdapter: LogAdapter
    private val logItems: MutableList<LogItem> by lazy {
        LogUtils.readLogData(this).toMutableList()
    }
    private var swipeToRemoveEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLogViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        swipeToRemoveEnabled = SettingsRepository(this).getBooleanValue(Constants.KEY_EDIT_LOG, false)

        setupRecyclerView()
        setupSwipeToRemove()

        if (swipeToRemoveEnabled)
            binding.btnClose.text = "로그 추가"
        binding.btnClose.setOnClickListener {
            if (swipeToRemoveEnabled)
                showAddLogDialog()
            else
                finish()
        }
        binding.ibBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        logAdapter = LogAdapter(logItems, swipeToRemoveEnabled)
        binding.rvLog.apply {
            layoutManager = LinearLayoutManager(this@LogViewActivity)
            adapter = logAdapter
        }
    }

    private fun setupSwipeToRemove() {
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (logAdapter.isSwipeToRemoveEnabled()) {
                    val position = viewHolder.bindingAdapterPosition
                    logAdapter.removeItem(position)
                } else {
                    logAdapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
                }
                LogUtils.saveLogData(this@LogViewActivity, logItems)
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return logAdapter.isSwipeToRemoveEnabled()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvLog)
    }

    fun setSwipeToRemoveEnabled(enabled: Boolean) {
        swipeToRemoveEnabled = enabled
        logAdapter.setSwipeToRemoveEnabled(enabled)
    }

    fun addLogItems(items: List<LogItem>) {
        logAdapter.updateItems(items)
    }

    fun showAddLogDialog() {
        val dialog = AddLogDialog(this) { newLogItem ->
            val insertionIdx = findInsertPosition(logItems, newLogItem)
            logItems.add(insertionIdx, newLogItem)
            logAdapter.notifyItemInserted(insertionIdx)
            binding.rvLog.scrollToPosition(insertionIdx)
            LogUtils.saveLogData(this, logItems)
        }
        dialog.show()
    }

    private fun <T : Comparable<T>> findInsertPosition(list: List<T>, item: T): Int {
        var left = 0
        var right = list.size

        while (left < right) {
            val mid = left + (right - left) / 2

            if (list[mid] < item) {
                left = mid + 1
            } else {
                right = mid
            }
        }

        return left
    }
}