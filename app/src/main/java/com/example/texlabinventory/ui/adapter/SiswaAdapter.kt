package com.example.texlabinventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.texlabinventory.data.model.Siswa
import com.example.texlabinventory.databinding.ItemSiswaBinding

class SiswaAdapter : RecyclerView.Adapter<SiswaAdapter.SiswaViewHolder>() {

    private val originalList = mutableListOf<Siswa>()
    private val filteredList = mutableListOf<Siswa>()

    fun setData(newList: List<Siswa>) {
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
                if (item.nama.lowercase().contains(searchPattern) ||
                    item.nis.lowercase().contains(searchPattern) ||
                    item.kelas.lowercase().contains(searchPattern)
                ) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
        onResult(filteredList.isEmpty())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiswaViewHolder {
        val binding = ItemSiswaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SiswaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SiswaViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    inner class SiswaViewHolder(private val binding: ItemSiswaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(siswa: Siswa) {
            binding.tvNamaSiswa.text = siswa.nama
            binding.tvNis.text = "NIS: ${siswa.nis}"
            binding.tvKelas.text = siswa.kelas
        }
    }
}