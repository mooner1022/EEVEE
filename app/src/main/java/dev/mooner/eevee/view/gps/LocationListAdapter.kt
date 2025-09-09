package dev.mooner.eevee.view.gps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import dev.mooner.eevee.LocationWithDistance
import dev.mooner.eevee.R
import kotlin.math.round

class LocationListAdapter(
    private val context: Context,
    private var items: List<LocationWithDistance>
): BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int =
        items.size

    override fun getItem(position: Int): Any =
        items[position]

    override fun getItemId(position: Int): Long =
        items[position].location.longHashCode()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.layout_list_location_item, parent, false)
        val item = items[position]
        val (location, distance) = item

        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val tvMap:  TextView = view.findViewById(R.id.tvMap)

        tvTitle.text = "${location.name}(${location.radius}m)"
        tvDistance.text = if (distance >= 1000.0) "${round(distance / 100) / 10}km" else "${distance.toInt()} m"
        tvMap.setOnClickListener {
            openMapScheme(location.latitude, location.longitude)
        }

        return view
    }

    fun updateItems(newItems: List<LocationWithDistance>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, newItem: LocationWithDistance) {
        if (position in 0 until items.size) {
            val mutableItems = items.toMutableList()
            mutableItems[position] = newItem
            items = mutableItems
            notifyDataSetChanged()
        }
    }

    private fun openMapScheme(lat: Double, lng: Double) {
        val uri = "geo:$lat,$lng?q=$lat,$lng"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        context.startActivity(intent)
    }
}