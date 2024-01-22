package com.example.calllogger

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyAdapter(private val context: Context, val logs: MutableList<MyCallLog>) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: MyCallLog)
    }

    var onItemClickListener: OnItemClickListener? = null

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView
        val type: TextView
        val number: TextView
        val duration: TextView

        init {
            name = itemView.findViewById(R.id.txtName)
            type = itemView.findViewById(R.id.txtType)
            number = itemView.findViewById(R.id.txtNumber)
            duration = itemView.findViewById(R.id.txtDuration)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.log_item, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val log = logs[position]

        // if there is no name put number
        holder.name.text = (if (logs[position].name.isNullOrBlank()) {
            logs[position].number
        } else {
            logs[position].name.toString()
        }).toString()
        // if name is not there then no need of number sub text
        holder.number.text = (if (holder.name.text.equals(logs[position].number)) {
            " "
        } else {
            logs[position].number
        }).toString()

        holder.type.text = modifiedType(logs[position].type.toInt())
        holder.duration.text = modifiedDuration(logs[position].duration.toInt())

        // set item click listener
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(logs[position])
        }
    }
    // convert seconds to MM:SS
    private fun modifiedDuration(duration: Int): CharSequence? {
        if (duration < 60) {
            return duration.toString()
        }
        val minutes = (duration / 60)
        val seconds = duration - minutes * 60
        return "$minutes:$seconds"
    }

    // Determine incoming or outgoing
    private fun modifiedType(type: Int): CharSequence {
        return when (type) {
            1 -> "Incoming"
            2 -> "Outgoing"
            else -> {
                " "
            }
        }
    }
}