package com.example.texlabinventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.texlabinventory.data.model.Guru
import com.example.texlabinventory.databinding.ItemGuruBinding

class GuruAdapter : RecyclerView.Adapter<GuruAdapter.GuruViewHolder>() {

    private val originalList = mutableListOf<Guru>()
    private val filteredList = mutableListOf<Guru>()

    fun setData(newList: List<Guru>) {
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
                if (item.nama_guru.lowercase().contains(searchPattern)) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
        onResult(filteredList.isEmpty())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuruViewHolder {
        val binding = ItemGuruBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GuruViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuruViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    inner class GuruViewHolder(private val binding: ItemGuruBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(guru: Guru) {
            binding.tvNamaGuru.text = guru.nama_guru
        }
    }
}