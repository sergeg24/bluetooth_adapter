package com.example.bluetoothadapter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothadapter.R
import com.example.bluetoothadapter.data.ListItem
import com.example.bluetoothadapter.databinding.ListItemBinding

class ItemSearchAdapter(private val listener: Listener) :
    RecyclerView.Adapter<ItemSearchAdapter.MyHolder>() {

    private val itemList = ArrayList<ListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return MyHolder(view, listener)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    class MyHolder(view: View, private val listener: Listener) : RecyclerView.ViewHolder(view) {

        private var device: ListItem? = null
        private val b = ListItemBinding.bind(view)

        init {
            b.rowDevice.setBackgroundResource(R.color.white)
            itemView.setOnClickListener {
                device?.let { listener.onClick(it) }
            }
        }

        fun bind(item: ListItem) = with(b) {
            device = item
            name.text = item.name
            address.text = item.address
            rssi.text = item.rssi.toString()
        }
    }

    fun clear() {
        itemList.clear()
        notifyDataSetChanged()
    }

    fun update(items: List<ListItem>) {
        clear()
        itemList.addAll(items)
        notifyDataSetChanged()
    }

    interface Listener {
        fun onClick(device: ListItem)
    }
}