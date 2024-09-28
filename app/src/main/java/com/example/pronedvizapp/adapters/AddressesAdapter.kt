package com.example.pronedvizapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.CallsInGroupAdapter.Companion.unixDateTimeToString
import com.example.pronedvizapp.requests.models.AddressInfo

class AddressesAdapter(private val context: Context, private val addresses: ArrayList<AddressInfo>) : BaseAdapter() {

    override fun getCount(): Int = addresses.size

    override fun getItem(position: Int): AddressInfo = addresses[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.address_listview_item, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.imageView3)
        val contentTextView = view.findViewById<TextView>(R.id.contentTextView)
        val dateTimeTextView = view.findViewById<TextView>(R.id.dateTimeTextView)

        imageView.setImageResource(R.drawable.baseline_location_pin_24)

        contentTextView.text = addresses[position].address
        dateTimeTextView.text = unixDateTimeToString(addresses[position].dateTime)

        return view
    }
}
