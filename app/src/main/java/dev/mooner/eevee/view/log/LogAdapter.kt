package dev.mooner.eevee.view.log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.mooner.eevee.databinding.LayoutListLogItemBinding
import dev.mooner.eevee.R
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter(
    private val logItems: MutableList<LogItem>,
    private var swipeToRemoveEnabled: Boolean = false
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun setSwipeToRemoveEnabled(enabled: Boolean) {
        swipeToRemoveEnabled = enabled
    }

    fun isSwipeToRemoveEnabled(): Boolean = swipeToRemoveEnabled

    fun removeItem(position: Int) {
        if (position >= 0 && position < logItems.size) {
            logItems.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItems(newItems: List<LogItem>) {
        logItems.clear()
        logItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = LayoutListLogItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logItems[position])
    }

    override fun getItemCount(): Int = logItems.size

    class LogViewHolder(private val binding: LayoutListLogItemBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(logItem: LogItem) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(logItem.timestamp))
            
            val (typeText, desc) = when(logItem.type) {
                LogItem.Type.LOCK              -> "기능 차단" to "수동"
                LogItem.Type.TRY_UNLOCK_BEACON -> "비콘기반 해제" to "시도"
                LogItem.Type.TRY_UNLOCK_GPS    -> "위치기반 해제(GPS)" to "시도"
                LogItem.Type.APP_UPDATE        -> "앱 업데이트" to logItem.desc
                LogItem.Type.INITIAL_INSTALL   -> "최초 설치" to null
                LogItem.Type.UNLOCK            -> "기능 허용" to logItem.desc
            }
            
            val displayText = buildString {
                append("[${formattedDate}]")
                append("\n")
                append(typeText)
                if (logItem.type == LogItem.Type.APP_UPDATE)
                    append(" : ").append(desc)
                else if (desc != null)
                    append(" | ").append(desc)
                append("\n")
                append("${logItem.androidVersion} | ${logItem.appVersion}")
            }
            
            binding.tvTitle.text = displayText
            if (logItem.type == LogItem.Type.LOCK /* || logItem.type == LogItem.Type.UNLOCK*/)
                binding.rlListItem.setBackgroundColor(binding.root.context.getColor(R.color.mdm_log_bg_red))
            else
                binding.rlListItem.setBackgroundColor(binding.root.context.getColor(R.color.mdm_drawer_background))
        }
    }
}