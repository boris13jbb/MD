package com.skyrunner.md.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skyrunner.md.data.HistorialCalculo
import com.skyrunner.md.databinding.ItemHistorialBinding
import com.skyrunner.md.utils.HistorialHelper
import com.skyrunner.md.utils.MathUtils

/**
 * Adapter para mostrar el historial de cálculos
 */
class HistorialAdapter(
    private val historial: List<HistorialCalculo>,
    private val historialHelper: HistorialHelper
) : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    inner class HistorialViewHolder(
        private val binding: ItemHistorialBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(calculo: HistorialCalculo) {
            binding.tvFactorHistorial.text = MathUtils.formatearDecimal(calculo.factorPromedio)
            binding.tvFechaHistorial.text = historialHelper.formatearFecha(calculo.fecha)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemHistorialBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(historial[position])
    }

    override fun getItemCount(): Int = historial.size
}
