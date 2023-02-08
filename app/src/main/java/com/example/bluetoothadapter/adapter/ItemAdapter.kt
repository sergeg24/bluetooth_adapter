package com.example.bluetoothadapter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothadapter.R
import com.example.bluetoothadapter.data.ListItem
import com.example.bluetoothadapter.databinding.ListItemBinding

class ItemAdapter(private val listener: Listener) :
    ListAdapter<ListItem, ItemAdapter.MyHolder>(Comparator()) {

    class MyHolder(view: View, private val adapter: ItemAdapter, private val listener: Listener) :
        RecyclerView.ViewHolder(view) {

        private val b = ListItemBinding.bind(view)
        private var device: ListItem? = null

        init {
            itemView.setOnClickListener {
                device?.let { it1 -> listener.onClick(it1) }
            }
        }

        fun bind(item: ListItem) = with(b) {
            device = item
            name.text = item.name
            address.text = item.address
            rssi.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return MyHolder(view, this, listener)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class Comparator : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(
            oldItem: ListItem,
            newItem: ListItem
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: ListItem,
            newItem: ListItem
        ): Boolean {
            return oldItem == newItem
        }
    }

    interface Listener {
        fun onClick(device: ListItem)
    }
}