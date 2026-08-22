package com.example.texlabinventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.texlabinventory.databinding.ItemImageSliderBinding

class ImageSliderAdapter(
    private val imageUrls: List<String>,
    private val onImageClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder>() {

    inner class SliderViewHolder(val binding: ItemImageSliderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val binding = ItemImageSliderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val url = imageUrls[position]

        // Load gambar ke ZoomImageView
        Glide.with(holder.itemView.context)
            .load(url)
            .into(holder.binding.ivSlider)

        if (onImageClick != null) {
            holder.binding.ivSlider.setOnClickListener {
                onImageClick.invoke(url)
            }
        } else {
            holder.binding.ivSlider.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}