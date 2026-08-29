package com.example.texlabinventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.texlabinventory.data.model.Ruang
import com.example.texlabinventory.databinding.ItemRuangBinding

class RuangAdapter : RecyclerView.Adapter<RuangAdapter.RuangViewHolder>() {

    private val originalList = mutableListOf<Ruang>()
    private val filteredList = mutableListOf<Ruang>()

    fun setData(newList: List<Ruang>) {
        originalList.clear()
        originalList.addAll(newList)
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    fun filter(query: String, onResult: (Boolean) -> Unit) {
        filteredList.clear()
        if (query.trim().isEmpty()) {
            filteredList.addAll(originalList)
        } else {
            val searchPattern = query.trim().lowercase()
            for (item in originalList) {
                if (item.nama_ruang.lowercase().contains(searchPattern)) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
        onResult(filteredList.isEmpty())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuangViewHolder {
        val binding = ItemRuangBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RuangViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuangViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    inner class RuangViewHolder(private val binding: ItemRuangBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ruang: Ruang) {
            binding.tvNamaRuang.text = ruang.nama_ruang
        }
    }
}