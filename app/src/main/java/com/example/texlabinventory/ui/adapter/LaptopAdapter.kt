package com.example.texlabinventory.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Laptop

class LaptopAdapter(
    private var laptopList: List<Laptop> = emptyList(),
    private val onItemClick: ((Laptop) -> Unit)? = null
) : RecyclerView.Adapter<LaptopAdapter.LaptopViewHolder>() {

    // Menyimpan copy list original untuk keperluan pencarian
    private var fullList: List<Laptop> = emptyList()

    class LaptopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivLaptop: ImageView = itemView.findViewById(R.id.ivLaptop)
        val tvInventoryId: TextView = itemView.findViewById(R.id.tvInventoryId)
        val tvBrandModel: TextView = itemView.findViewById(R.id.tvBrandModel)
        val tvSerialNumber: TextView = itemView.findViewById(R.id.tvSerialNumber)
        val tvSpecs: TextView = itemView.findViewById(R.id.tvSpecs)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvCondition: TextView = itemView.findViewById(R.id.tvCondition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaptopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_laptop, parent, false)
        return LaptopViewHolder(view)
    }

    override fun onBindViewHolder(holder: LaptopViewHolder, position: Int) {
        val laptop = laptopList[position]

        val firstImageUrl = laptop.image_url.firstOrNull()

        // 1. Baris Paling Atas: Kode ID Inventaris
        holder.tvInventoryId.text = laptop.inventory_id

        // 2. Baris Kedua: Merk & Model
        holder.tvBrandModel.text = "${laptop.brand} ${laptop.model}".trim()

        // 3. Baris Ketiga: Serial Number (Terpisah di Bawahnya)
        holder.tvSerialNumber.text = "SN: ${laptop.serial_number}"

        // 4. Spesifikasi
        holder.tvSpecs.text = "${laptop.specs.processor} • ${laptop.specs.ram} • ${laptop.specs.storage}"

        // 5. Lokasi & Kondisi
        holder.tvLocation.text = "📍 ${laptop.location}"
        holder.tvCondition.text = laptop.condition

        // Gambar dari Cloudinary
        if (!firstImageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(firstImageUrl) // Load URL foto pertama dari List
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery) // Gambar default saat loading (jika ada)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.ivLaptop) // Sesuaikan id ImageView kamu di item_laptop.xml
        } else {
            // Jika laptop belum punya foto, tampilkan gambar placeholder default
            holder.ivLaptop.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(laptop)
        }

    }

    override fun getItemCount(): Int = laptopList.size

    fun updateData(newList: List<Laptop>) {
        this.fullList = newList
        this.laptopList = newList
        notifyDataSetChanged()
    }

    // Fungsi untuk memfilter list secara real-time berdasarkan query
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

        // Panggil callback untuk memberitahu apakah hasil pencarian kosong
        onEmptyResult(filteredList.isEmpty())
    }
}