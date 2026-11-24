package com.docscan.privacy.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.docscan.privacy.data.ScannedDoc
import com.docscan.privacy.databinding.ItemDocBinding

class DocAdapter(
    private var items: List<ScannedDoc>,
    private val onDeleteClick: (ScannedDoc) -> Unit
) : RecyclerView.Adapter<DocAdapter.DocViewHolder>() {

    inner class DocViewHolder(val binding: ItemDocBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        val binding = ItemDocBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ScannedDoc>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvDocTitle.text = item.title
        holder.binding.tvDocDate.text = "${item.date} • ${item.category}"
        holder.binding.tvDocAmount.text = String.format("%s%.2f", item.currency, item.totalAmount)

        val icon = when (item.category) {
            "GROCERIES" -> "🛒"
            "DINING" -> "☕"
            "FUEL" -> "⛽"
            "TECH" -> "💻"
            "PHARMACY" -> "💊"
            else -> "📄"
        }
        holder.binding.tvCategoryIcon.text = icon

        holder.binding.btnDeleteDoc.setOnClickListener {
            onDeleteClick(item)
        }
    }
}
