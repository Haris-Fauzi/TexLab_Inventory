package com.example.texlabinventory.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.databinding.ItemLaptopBinding

class LaptopAdapter(
    private var laptopList: List<Laptop> = emptyList(),
    private val onItemClick: ((Laptop) -> Unit)? = null
) : RecyclerView.Adapter<LaptopAdapter.LaptopViewHolder>() {

    private var fullList: List<Laptop> = emptyList()

    // ViewHolder menggunakan ItemLaptopBinding
    class LaptopViewHolder(val binding: ItemLaptopBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaptopViewHolder {
        val binding = ItemLaptopBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LaptopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LaptopViewHolder, position: Int) {
        val laptop = laptopList[position]

        with(holder.binding) {
            // 1. ID Inventaris
            tvInventoryId.text = laptop.inventory_id

            // 2. Merek & Model
            val brandModelText = if (laptop.brand.isNotEmpty()) {
                "${laptop.brand} ${laptop.model}".trim()
            } else {
                laptop.model
            }
            tvBrandModel.text = brandModelText

            // 3. Serial Number
            tvSerialNumber.text = "SN: ${laptop.serial_number}"

            // 4. Spesifikasi
            tvSpecs.text = "${laptop.specs.processor} • ${laptop.specs.ram} • ${laptop.specs.storage}"

            // 5. Lokasi & Kondisi
            tvLocation.text = "📍 ${laptop.location}"
            tvCondition.text = "Kondisi: ${laptop.condition.ifEmpty { "-" }}"

            // 6. Status Peminjaman (DIPINJAM / TERSEDIA)
            val isDipinjam = laptop.status.equals("DIPINJAM", ignoreCase = true)
            tvStatus.text = if (isDipinjam) "DIPINJAM" else "TERSEDIA"

            // Warna Badge Status (Merah = Dipinjam, Hijau = Tersedia)
            val statusColorRes = if (isDipinjam) R.color.status_dipinjam else R.color.status_tersedia
            tvStatus.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(root.context, statusColorRes)

            // 7. Gambar
            val firstImageUrl = laptop.image_url.firstOrNull()
            if (!firstImageUrl.isNullOrEmpty()) {
                Glide.with(root.context)
                    .load(firstImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .into(ivLaptop)
            } else {
                ivLaptop.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Click Listener
            root.setOnClickListener {
                onItemClick?.invoke(laptop)
            }
        }
    }

    override fun getItemCount(): Int = laptopList.size

    fun updateData(newList: List<Laptop>) {
        this.fullList = newList
        this.laptopList = newList
        notifyDataSetChanged()
    }

    fun filter(query: String, onEmptyResult: (Boolean) -> Unit) {
        val filteredList = if (query.isEmpty()) {
            fullList
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            fullList.filter { laptop ->
                laptop.inventory_id.lowercase().contains(lowerCaseQuery) ||
                        laptop.brand.lowercase().contains(lowerCaseQuery) ||
                        laptop.model.lowercase().contains(lowerCaseQuery) ||
                        laptop.serial_number.lowercase().contains(lowerCaseQuery) ||
                        laptop.location.lowercase().contains(lowerCaseQuery)
            }
        }

        this.laptopList = filteredList
        notifyDataSetChanged()
        onEmptyResult(filteredList.isEmpty())
    }
}