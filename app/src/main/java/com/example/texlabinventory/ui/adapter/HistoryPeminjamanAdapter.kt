package com.example.texlabinventory.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Peminjaman
import com.example.texlabinventory.databinding.ItemHistoryPeminjamanBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryPeminjamanAdapter : ListAdapter<Peminjaman, HistoryPeminjamanAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(private val binding: ItemHistoryPeminjamanBinding) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

        fun bind(item: Peminjaman) = with(binding) {
            tvNamaItem.text = "${item.namaItem} (${item.itemId})"
            tvNamaSiswa.text = "👤 Peminjam: ${item.namaSiswa} (NIS: ${item.siswaId})"
            tvKelasRuangan.text = "🏫 Kelas: ${item.kelasSiswa} | Ruang: ${item.ruangan}"
            tvGuruPengajar.text = "👨‍🏫 Guru: ${item.guruPengajar}"

            val pinjamStr = item.waktuPinjam?.toDate()?.let { dateFormat.format(it) } ?: "-"
            tvWaktuPinjam.text = "📅 Pinjam: $pinjamStr"

            if (item.status.equals("DIKEMBALIKAN", ignoreCase = true)) {
                val kembaliStr = item.waktuKembali?.toDate()?.let { dateFormat.format(it) } ?: "-"
                tvWaktuKembali.text = "📅 Kembali: $kembaliStr"
                tvWaktuKembali.visibility = View.VISIBLE

                tvStatusBadge.text = "DIKEMBALIKAN"
                tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50")) // Hijau
            } else {
                tvWaktuKembali.visibility = View.GONE
                tvStatusBadge.text = "DIPINJAM"
                tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800")) // Oranye
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryPeminjamanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Peminjaman>() {
            override fun areItemsTheSame(oldItem: Peminjaman, newItem: Peminjaman): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Peminjaman, newItem: Peminjaman): Boolean {
                return oldItem == newItem
            }
        }
    }
}