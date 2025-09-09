package dev.mooner.eevee.view.drawer

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import coil3.load
import dev.mooner.eevee.R

class DrawerListAdapter(
    private val context: Context,
    private var items: List<DrawerItem>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.item_list_drawer, parent, false)
        val item = items[position]

        val rlContainer = view.findViewById<RelativeLayout>(R.id.rlContainer)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvValue = view.findViewById<TextView>(R.id.tvValue)
        val swState = view.findViewById<Switch>(R.id.swState)
        val ivDetail = view.findViewById<ImageView>(R.id.ivDetail)
        val tvDetail = view.findViewById<TextView>(R.id.tvDetail)

        // Set title
        tvTitle.text = item.title

        // Set icon
        if (item.iconRes != null) {
            ivIcon.load(item.iconRes!!) {
                if (item.iconRes == R.drawable.img_icon_check)
                    size(14)
                else
                    size(36)
            }
            ivIcon.visibility = View.VISIBLE
        } else {
            ivIcon.visibility = View.GONE
        }

        // Reset all views to default state
        tvValue.visibility = View.GONE
        swState.visibility = View.GONE
        ivDetail.visibility = View.GONE
        tvDetail.visibility = View.GONE
        tvTitle.setTextColor(context.getColor(R.color.white))
        tvTitle.setPadding(0)
        rlContainer.setBackgroundColor(context.getColor(R.color.transparent))
        swState.setOnCheckedChangeListener(null)
        view.setOnClickListener(null)

        // Configure based on item type
        when (item) {
            is SwitchDrawerItem -> {
                // Show value text if available
                if (item.value != null) {
                    tvValue.text = item.value
                    tvValue.visibility = View.VISIBLE
                }
                
                // Show switch
                swState.visibility = View.VISIBLE
                swState.isChecked = item.switchState
                
                swState.setOnCheckedChangeListener { _, isChecked ->
                    item.onSwitchToggle?.invoke(item, isChecked)
                }
            }
            
            is ArrowDrawerItem -> {
                // Show detail arrow
                ivDetail.visibility = View.VISIBLE
                if (item.value != null) {
                    tvValue.text = item.value
                    tvValue.visibility = View.VISIBLE
                }
                
                // Set click listener
                view.setOnClickListener {
                    if (item.isClickable) {
                        item.onItemClick?.invoke(item)
                    }
                }
            }
            
            is TextDrawerItem -> {
                // Show detail text
                tvDetail.visibility = View.VISIBLE
                tvDetail.text = item.detailText
                
                // Set click listener
                view.setOnClickListener {
                    if (item.isClickable) {
                        item.onItemClick?.invoke(item)
                    }
                }
            }
            
            is HeaderDrawerItem -> {
                tvTitle.text = item.title
                tvTitle.updatePadding(left = 50)
                tvTitle.setTextColor(Color.parseColor("#a1a1a1"))
                rlContainer.setBackgroundColor(context.getColor(R.color.mdm_drawer_header_background))
            }
        }

        return view
    }

    fun updateItems(newItems: List<DrawerItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, newItem: DrawerItem) {
        if (position in 0 until items.size) {
            val mutableItems = items.toMutableList()
            mutableItems[position] = newItem
            items = mutableItems
            notifyDataSetChanged()
        }
    }
}