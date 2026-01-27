package com.skyrunner.md.adapter

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.skyrunner.md.R
import com.skyrunner.md.data.RegistroData
import com.skyrunner.md.databinding.ItemRegistroBinding
import com.skyrunner.md.utils.MathUtils

/**
 * Adapter para el RecyclerView que muestra los registros de datos
 * Maneja la entrada de Peso Bruto y Metros, y muestra el Factor calculado
 */
class RegistroAdapter(
    private val registros: MutableList<RegistroData>,
    private val onRegistroChanged: (Int, Double, Double) -> Unit,
    private val onRegistroDeleted: (Int) -> Unit
) : RecyclerView.Adapter<RegistroAdapter.RegistroViewHolder>() {

    inner class RegistroViewHolder(
        private val binding: ItemRegistroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var pesoWatcher: TextWatcher? = null
        private var metrosWatcher: TextWatcher? = null

        fun bind(registro: RegistroData, position: Int) {
            // Remover watchers anteriores para evitar loops infinitos
            binding.etPesoBruto.removeTextChangedListener(pesoWatcher)
            binding.etMetros.removeTextChangedListener(metrosWatcher)

            // Guardar el foco y posición del cursor antes de actualizar
            val pesoBrutoTieneFoco = binding.etPesoBruto.hasFocus()
            val metrosTieneFoco = binding.etMetros.hasFocus()
            val pesoBrutoCursorPos = binding.etPesoBruto.selectionStart
            val metrosCursorPos = binding.etMetros.selectionStart

            // Mostrar número de fila
            binding.tvNumeroFila.text = "Fila ${position + 1}"

            // Establecer valores iniciales solo si son diferentes y el campo NO tiene foco
            // Esto evita interrumpir la escritura del usuario
            if (!pesoBrutoTieneFoco) {
                val pesoActual = binding.etPesoBruto.text.toString().toDoubleOrNull() ?: 0.0
                if (pesoActual != registro.pesoBruto) {
                    if (registro.pesoBruto > 0) {
                        binding.etPesoBruto.setText(registro.pesoBruto.toString())
                    } else {
                        binding.etPesoBruto.setText("")
                    }
                }
            }

            if (!metrosTieneFoco) {
                val metrosActual = binding.etMetros.text.toString().toDoubleOrNull() ?: 0.0
                if (metrosActual != registro.metros) {
                    if (registro.metros > 0) {
                        binding.etMetros.setText(registro.metros.toString())
                    } else {
                        binding.etMetros.setText("")
                    }
                }
            }

            // Mostrar factor calculado
            if (registro.factor > 0) {
                binding.tvFactor.text = MathUtils.formatearDecimal(registro.factor)
            } else {
                binding.tvFactor.text = "0.0000"
            }

            // Aplicar estilo visual si es outlier
            if (registro.isOutlier) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.outlier_background)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
            }

            // Configurar watchers para actualizar en tiempo real
            // Solo actualizamos el factor visualmente mientras se escribe
            // El ViewModel se actualiza cuando el campo pierde el foco para evitar loops
            pesoWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val texto = s?.toString() ?: ""
                    val peso = texto.toDoubleOrNull() ?: 0.0
                    val metros = binding.etMetros.text.toString().toDoubleOrNull() ?: 0.0
                    
                    // Calcular factor localmente para mostrar inmediatamente
                    val factor = if (peso > 0) metros / peso else 0.0
                    binding.tvFactor.text = MathUtils.formatearDecimal(factor)
                    
                    // NO actualizar el ViewModel aquí para evitar loops
                    // Se actualizará cuando el campo pierda el foco
                }
            }

            metrosWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val texto = s?.toString() ?: ""
                    val peso = binding.etPesoBruto.text.toString().toDoubleOrNull() ?: 0.0
                    val metros = texto.toDoubleOrNull() ?: 0.0
                    
                    // Calcular factor localmente para mostrar inmediatamente
                    val factor = if (peso > 0) metros / peso else 0.0
                    binding.tvFactor.text = MathUtils.formatearDecimal(factor)
                    
                    // NO actualizar el ViewModel aquí para evitar loops
                    // Se actualizará cuando el campo pierda el foco
                }
            }

            binding.etPesoBruto.addTextChangedListener(pesoWatcher)
            binding.etMetros.addTextChangedListener(metrosWatcher)
            
            // Actualizar el ViewModel cuando el campo pierde el foco
            binding.etPesoBruto.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val peso = binding.etPesoBruto.text.toString().toDoubleOrNull() ?: 0.0
                    val metros = binding.etMetros.text.toString().toDoubleOrNull() ?: 0.0
                    onRegistroChanged(position, peso, metros)
                }
            }
            
            binding.etMetros.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val peso = binding.etPesoBruto.text.toString().toDoubleOrNull() ?: 0.0
                    val metros = binding.etMetros.text.toString().toDoubleOrNull() ?: 0.0
                    onRegistroChanged(position, peso, metros)
                }
            }

            // Configurar botón de eliminar
            binding.btnEliminar.setOnClickListener {
                onRegistroDeleted(position)
            }

            // Mostrar/ocultar botón eliminar según si hay más de una fila
            binding.btnEliminar.visibility = if (registros.size > 1) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistroViewHolder {
        val binding = ItemRegistroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RegistroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegistroViewHolder, position: Int) {
        holder.bind(registros[position], position)
    }

    override fun getItemCount(): Int = registros.size

    /**
     * Actualiza la lista de registros
     * Evita actualizar items que están siendo editados actualmente
     */
    fun actualizarRegistros(nuevaLista: List<RegistroData>) {
        // Guardar la lista anterior para comparar
        val listaAnterior = registros.toList()
        
        // Solo actualizar si la lista realmente cambió
        if (listaAnterior.size != nuevaLista.size || listaAnterior != nuevaLista) {
            val oldSize = listaAnterior.size
            registros.clear()
            registros.addAll(nuevaLista)
            
            // Usar post() para retrasar la notificación hasta que el RecyclerView termine su layout
            // Esto evita el error "Cannot call this method while RecyclerView is computing a layout"
            Handler(Looper.getMainLooper()).post {
                try {
                    // Usar notifyItemChanged para evitar perder el foco en los campos
                    if (oldSize == nuevaLista.size) {
                        // Si el tamaño es igual, solo notificar cambios en items específicos
                        // pero solo si el valor realmente cambió significativamente
                        for (i in nuevaLista.indices) {
                            if (i < oldSize) {
                                val anterior = listaAnterior[i]
                                val nuevo = nuevaLista[i]
                                // Solo actualizar si cambió el factor o el estado de outlier
                                // No actualizar si solo cambió ligeramente el peso o metros (el usuario está escribiendo)
                                val cambioSignificativo = anterior.factor != nuevo.factor || 
                                                         anterior.isOutlier != nuevo.isOutlier ||
                                                         Math.abs(anterior.pesoBruto - nuevo.pesoBruto) > 0.001 ||
                                                         Math.abs(anterior.metros - nuevo.metros) > 0.001
                                if (cambioSignificativo) {
                                    notifyItemChanged(i)
                                }
                            }
                        }
                    } else {
                        // Si el tamaño cambió, usar notifyDataSetChanged
                        notifyDataSetChanged()
                    }
                } catch (e: IllegalStateException) {
                    // Si aún hay un error (RecyclerView está en layout), usar notifyDataSetChanged como fallback
                    notifyDataSetChanged()
                }
            }
        }
    }
}
