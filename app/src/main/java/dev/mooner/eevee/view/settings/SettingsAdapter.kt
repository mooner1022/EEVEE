package dev.mooner.eevee.view.settings

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.DatePicker
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.mooner.eevee.R
import dev.mooner.eevee.utils.TimeUtils
import java.util.Calendar

class SettingsAdapter(
    private val viewModel: SettingsViewModel,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var settingItems = mutableListOf<SettingItem>()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SWITCH = 1
        private const val VIEW_TYPE_LIST = 2
        private const val VIEW_TYPE_CUSTOM = 3
        private const val VIEW_TYPE_TEXT = 4
        private const val VIEW_TYPE_SLIDER = 5
        private const val VIEW_TYPE_DATETIME = 6
    }

    fun updateSettings(settingGroups: List<SettingGroup>) {
        settingItems.clear()
        settingGroups.forEach { group ->
            settingItems.add(HeaderSetting(group.title))
            settingItems.addAll(group.settings)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (settingItems[position].type) {
            SettingType.HEADER -> VIEW_TYPE_HEADER
            SettingType.SWITCH -> VIEW_TYPE_SWITCH
            SettingType.LIST -> VIEW_TYPE_LIST
            SettingType.CUSTOM -> VIEW_TYPE_CUSTOM
            SettingType.TEXT -> VIEW_TYPE_TEXT
            SettingType.SLIDER -> VIEW_TYPE_SLIDER
            SettingType.DATE_TIME -> VIEW_TYPE_DATETIME
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_SWITCH -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_switch, parent, false)
                SwitchViewHolder(view)
            }
            VIEW_TYPE_LIST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_list, parent, false)
                ListViewHolder(view)
            }
            VIEW_TYPE_TEXT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_text, parent, false)
                TextViewHolder(view)
            }
            VIEW_TYPE_SLIDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_slider, parent, false)
                SliderViewHolder(view)
            }
            VIEW_TYPE_DATETIME -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_datetime, parent, false)
                DateTimeViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_custom, parent, false)
                CustomViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val setting = settingItems[position]

        when (holder) {
            is HeaderViewHolder -> holder.bind(setting as HeaderSetting)
            is SwitchViewHolder -> holder.bind(setting as SwitchSetting, viewModel)
            is ListViewHolder -> holder.bind(setting as ListSetting, viewModel, context)
            is TextViewHolder -> holder.bind(setting as TextSetting, viewModel, context)
            is SliderViewHolder -> holder.bind(setting as SliderSetting, viewModel)
            is DateTimeViewHolder -> holder.bind(setting as DateTimeSetting, viewModel, context)
            is CustomViewHolder -> holder.bind(setting as CustomSetting)
        }
    }

    override fun getItemCount(): Int = settingItems.size

    // ViewHolder 클래스들
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.headerTitle)

        fun bind(setting: HeaderSetting) {
            titleText.text = setting.title
        }
    }

    class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.settingTitle)
        private val summaryText: TextView = itemView.findViewById(R.id.settingSummary)
        private val switchWidget: SwitchCompat = itemView.findViewById(R.id.settingSwitch)

        fun bind(setting: SwitchSetting, viewModel: SettingsViewModel) {
            titleText.text = setting.title
            summaryText.text = setting.summary
            summaryText.visibility = if (setting.summary.isNullOrEmpty()) View.GONE else View.VISIBLE

            val currentValue = viewModel.getBooleanSetting(setting.key, setting.defaultValue)
            switchWidget.isChecked = currentValue

            switchWidget.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateBooleanSetting(setting.key, isChecked)
            }

            itemView.setOnClickListener {
                switchWidget.isChecked = !switchWidget.isChecked
            }
        }
    }

    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.settingTitle)
        private val summaryText: TextView = itemView.findViewById(R.id.settingSummary)

        fun bind(setting: ListSetting, viewModel: SettingsViewModel, context: Context) {
            titleText.text = setting.title

            val currentValue = viewModel.getStringSetting(setting.key, setting.defaultValue)
            val currentIndex = setting.entryValues.indexOf(currentValue)
            val currentEntry = if (currentIndex != -1) setting.entries[currentIndex] else setting.entries[0]

            summaryText.text = currentEntry

            itemView.setOnClickListener {
                showListDialog(setting, viewModel, context)
            }
        }

        private fun showListDialog(setting: ListSetting, viewModel: SettingsViewModel, context: Context) {
            val bottomSheetDialog = BottomSheetDialog(context)
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_list_selection, null)
            
            val title = view.findViewById<TextView>(R.id.bottomSheetTitle)
            val recyclerView = view.findViewById<RecyclerView>(R.id.optionsList)
            
            title.text = setting.title
            
            val currentValue = viewModel.getStringSetting(setting.key, setting.defaultValue)
            val adapter = BottomSheetListAdapter(setting, currentValue) { selectedValue ->
                viewModel.updateStringSetting(setting.key, selectedValue)
                
                // Update the displayed value immediately
                val selectedIndex = setting.entryValues.indexOf(selectedValue)
                val selectedEntry = if (selectedIndex != -1) setting.entries[selectedIndex] else setting.entries[0]
                summaryText.text = selectedEntry
                
                bottomSheetDialog.dismiss()
            }
            
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            
            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()
        }
    }

    class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.settingTitle)
        private val summaryText: TextView = itemView.findViewById(R.id.settingSummary)

        fun bind(setting: CustomSetting) {
            titleText.text = setting.title
            summaryText.text = setting.summary
            summaryText.visibility = if (setting.summary.isNullOrEmpty()) View.GONE else View.VISIBLE

            itemView.setOnClickListener {
                setting.customAction.invoke(it)
            }
        }
    }

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.settingTitle)
        private val summaryText: TextView = itemView.findViewById(R.id.settingSummary)
        private val valueText: TextView = itemView.findViewById(R.id.settingValue)

        fun bind(setting: TextSetting, viewModel: SettingsViewModel, context: Context) {
            titleText.text = setting.title
            summaryText.text = setting.summary
            summaryText.visibility = if (setting.summary.isNullOrEmpty()) View.GONE else View.VISIBLE
            
            val currentValue = viewModel.getStringSetting(setting.key, setting.defaultValue)
            valueText.text = currentValue.ifEmpty { setting.hint ?: "" }
            
            itemView.setOnClickListener {
                showTextInputDialog(setting, viewModel, context)
            }
        }
        
        private fun showTextInputDialog(setting: TextSetting, viewModel: SettingsViewModel, context: Context) {
            val bottomSheetDialog = BottomSheetDialog(context)
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_text_input, null)
            
            val title = view.findViewById<TextView>(R.id.bottomSheetTitle)
            val textInputLayout = view.findViewById<TextInputLayout>(R.id.textInputLayout)
            val editText = view.findViewById<TextInputEditText>(R.id.textInputEditText)
            val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
            val confirmButton = view.findViewById<MaterialButton>(R.id.confirmButton)
            
            title.text = setting.title
            textInputLayout.hint = setting.hint
            editText.setText(viewModel.getStringSetting(setting.key, setting.defaultValue))
            editText.inputType = setting.inputType
            
            cancelButton.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
            
            confirmButton.setOnClickListener {
                val newValue = editText.text.toString()
                viewModel.updateStringSetting(setting.key, newValue)
                
                // Update the displayed value immediately
                valueText.text = if (newValue.isEmpty()) setting.hint ?: "" else newValue
                
                bottomSheetDialog.dismiss()
            }
            
            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()
        }
    }
    
    class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.settingTitle)
        private val summaryText: TextView = itemView.findViewById(R.id.settingSummary)
        private val valueText: TextView = itemView.findViewById(R.id.settingValue)
        private val seekBar: SeekBar = itemView.findViewById(R.id.settingSeekBar)

        fun bind(setting: SliderSetting, viewModel: SettingsViewModel) {
            titleText.text = setting.title
            summaryText.text = setting.summary
            summaryText.visibility = if (setting.summary.isNullOrEmpty()) View.GONE else View.VISIBLE
            
            val currentValue = viewModel.getIntSetting(setting.key, setting.defaultValue)
            seekBar.max = (setting.maxValue - setting.minValue) / setting.stepSize
            seekBar.progress = (currentValue - setting.minValue) / setting.stepSize
            
            updateValueText(currentValue, setting.unit)
            
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val value = setting.minValue + (progress * setting.stepSize)
                        updateValueText(value, setting.unit)
                        viewModel.updateIntSetting(setting.key, value)
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        private fun updateValueText(value: Int, unit: String?) {
            valueText.text = if (unit != null) "$value $unit" else value.toString()
        }
    }
    
    class DateTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.settingTitle)
        private val summaryText: TextView = itemView.findViewById(R.id.settingSummary)
        private val valueText: TextView = itemView.findViewById(R.id.settingValue)

        fun bind(setting: DateTimeSetting, viewModel: SettingsViewModel, context: Context) {
            titleText.text = setting.title
            summaryText.text = setting.summary
            summaryText.visibility = if (setting.summary.isNullOrEmpty()) View.GONE else View.VISIBLE
            
            val currentValue = viewModel.getLongSetting(setting.key, setting.defaultValue)
            valueText.text = TimeUtils.formatMillis(currentValue, setting.dateFormat)
            
            itemView.setOnClickListener {
                showDateTimePickerDialog(setting, viewModel, context)
            }
        }
        
        private fun showDateTimePickerDialog(setting: DateTimeSetting, viewModel: SettingsViewModel, context: Context) {
            val bottomSheetDialog = BottomSheetDialog(context)
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_datetime_picker, null)
            
            val title = view.findViewById<TextView>(R.id.bottomSheetTitle)
            val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
            val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
            val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
            val confirmButton = view.findViewById<MaterialButton>(R.id.confirmButton)
            
            title.text = setting.title
            
            val currentValue = viewModel.getLongSetting(setting.key, setting.defaultValue)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentValue
            
            val adapter = DateTimePickerAdapter(context as FragmentActivity, calendar, setting.use24HourFormat)
            viewPager.adapter = adapter
            
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = if (position == 0) "날짜" else "시간"
            }.attach()
            
            cancelButton.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
            
            confirmButton.setOnClickListener {
                val selectedTimestamp = adapter.getSelectedDateTime()
                viewModel.updateLongSetting(setting.key, selectedTimestamp)
                
                valueText.text = TimeUtils.formatMillis(selectedTimestamp, setting.dateFormat)
                
                bottomSheetDialog.dismiss()
            }
            
            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()
        }
    }
    
    class DateTimePickerAdapter(
        private val fragmentActivity: FragmentActivity,
        private val initialCalendar: Calendar,
        private val use24HourFormat: Boolean
    ) : FragmentStateAdapter(fragmentActivity) {
        
        private var datePickerFragment: DatePickerFragment? = null
        private var timePickerFragment: TimePickerFragment? = null
        
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    datePickerFragment = DatePickerFragment.newInstance(initialCalendar)
                    datePickerFragment!!
                }
                1 -> {
                    timePickerFragment = TimePickerFragment.newInstance(initialCalendar, use24HourFormat)
                    timePickerFragment!!
                }
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
        
        fun getSelectedDateTime(): Long {
            val calendar = Calendar.getInstance()
            
            datePickerFragment?.let { dateFragment ->
                calendar.set(Calendar.YEAR, dateFragment.getSelectedYear())
                calendar.set(Calendar.MONTH, dateFragment.getSelectedMonth())
                calendar.set(Calendar.DAY_OF_MONTH, dateFragment.getSelectedDay())
            }
            
            timePickerFragment?.let { timeFragment ->
                calendar.set(Calendar.HOUR_OF_DAY, timeFragment.getSelectedHour())
                calendar.set(Calendar.MINUTE, timeFragment.getSelectedMinute())
                calendar.set(Calendar.SECOND, timeFragment.getSelectedSecond())
                calendar.set(Calendar.MILLISECOND, 0)
            }
            
            return calendar.timeInMillis
        }
    }
    
    class DatePickerFragment : Fragment() {
        private lateinit var datePicker: DatePicker
        
        companion object {
            fun newInstance(calendar: Calendar): DatePickerFragment {
                val fragment = DatePickerFragment()
                fragment.arguments = android.os.Bundle().apply {
                    putInt("year", calendar.get(Calendar.YEAR))
                    putInt("month", calendar.get(Calendar.MONTH))
                    putInt("day", calendar.get(Calendar.DAY_OF_MONTH))
                }
                return fragment
            }
        }
        
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_date_picker, container, false)
            datePicker = view.findViewById(R.id.datePicker)
            
            arguments?.let { args ->
                val year = args.getInt("year")
                val month = args.getInt("month")
                val day = args.getInt("day")
                datePicker.init(year, month, day, null)
            }
            
            return view
        }
        
        fun getSelectedYear(): Int = datePicker.year
        fun getSelectedMonth(): Int = datePicker.month
        fun getSelectedDay(): Int = datePicker.dayOfMonth
    }
    
    class TimePickerFragment : Fragment() {
        private lateinit var timePicker: TimePicker
        private lateinit var secondsPicker: NumberPicker
        
        companion object {
            fun newInstance(calendar: Calendar, use24HourFormat: Boolean): TimePickerFragment {
                val fragment = TimePickerFragment()
                fragment.arguments = android.os.Bundle().apply {
                    putInt("hour", calendar.get(Calendar.HOUR_OF_DAY))
                    putInt("minute", calendar.get(Calendar.MINUTE))
                    putInt("second", calendar.get(Calendar.SECOND))
                    putBoolean("use24HourFormat", use24HourFormat)
                }
                return fragment
            }
        }
        
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_time_picker, container, false)
            timePicker = view.findViewById(R.id.timePicker)
            secondsPicker = view.findViewById(R.id.secondsPicker)
            
            // Setup seconds picker
            secondsPicker.minValue = 0
            secondsPicker.maxValue = 59
            secondsPicker.setFormatter { String.format("%02d", it) }
            
            arguments?.let { args ->
                val hour = args.getInt("hour")
                val minute = args.getInt("minute")
                val second = args.getInt("second", 0)
                val use24HourFormat = args.getBoolean("use24HourFormat", true)
                
                timePicker.setIs24HourView(use24HourFormat)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    timePicker.hour = hour
                    timePicker.minute = minute
                } else {
                    @Suppress("DEPRECATION")
                    timePicker.currentHour = hour
                    @Suppress("DEPRECATION")
                    timePicker.currentMinute = minute
                }
                
                secondsPicker.value = second
            }
            
            return view
        }
        
        fun getSelectedHour(): Int = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.hour
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour
        }
        
        fun getSelectedMinute(): Int = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.minute
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentMinute
        }
        
        fun getSelectedSecond(): Int = secondsPicker.value
    }
    
    class BottomSheetListAdapter(
        private val setting: ListSetting,
        private val currentValue: String,
        private val onItemSelected: (String) -> Unit
    ) : RecyclerView.Adapter<BottomSheetListAdapter.OptionViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bottom_sheet_option, parent, false)
            return OptionViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
            holder.bind(setting.entries[position], setting.entryValues[position], currentValue)
        }
        
        override fun getItemCount(): Int = setting.entries.size
        
        inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val radioButton: RadioButton = itemView.findViewById(R.id.radioButton)
            private val optionText: TextView = itemView.findViewById(R.id.optionText)
            
            fun bind(entry: String, entryValue: String, currentValue: String) {
                optionText.text = entry
                radioButton.isChecked = entryValue == currentValue
                
                itemView.setOnClickListener {
                    onItemSelected(entryValue)
                }
            }
        }
    }
}