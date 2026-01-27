package com.skyrunner.md.adapter

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

            // Establecer valores iniciales solo si son diferentes para evitar loops
            val pesoActual = binding.etPesoBruto.text.toString().toDoubleOrNull() ?: 0.0
            val metrosActual = binding.etMetros.text.toString().toDoubleOrNull() ?: 0.0
            
            if (pesoActual != registro.pesoBruto) {
                if (registro.pesoBruto > 0) {
                    binding.etPesoBruto.setText(registro.pesoBruto.toString())
                } else {
                    binding.etPesoBruto.setText("")
                }
                // Restaurar posición del cursor si el campo tenía foco
                if (pesoBrutoTieneFoco && pesoBrutoCursorPos >= 0) {
                    val nuevaPos = minOf(pesoBrutoCursorPos, binding.etPesoBruto.text?.length ?: 0)
                    binding.etPesoBruto.setSelection(nuevaPos)
                }
            }

            if (metrosActual != registro.metros) {
                if (registro.metros > 0) {
                    binding.etMetros.setText(registro.metros.toString())
                } else {
                    binding.etMetros.setText("")
                }
                // Restaurar posición del cursor si el campo tenía foco
                if (metrosTieneFoco && metrosCursorPos >= 0) {
                    val nuevaPos = minOf(metrosCursorPos, binding.etMetros.text?.length ?: 0)
                    binding.etMetros.setSelection(nuevaPos)
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
            pesoWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val texto = s?.toString() ?: ""
                    if (texto.isEmpty()) {
                        // Si está vacío, mostrar 0.0000 pero no notificar aún
                        binding.tvFactor.text = "0.0000"
                        onRegistroChanged(position, 0.0, binding.etMetros.text.toString().toDoubleOrNull() ?: 0.0)
                        return
                    }
                    
                    val peso = texto.toDoubleOrNull() ?: 0.0
                    val metros = binding.etMetros.text.toString().toDoubleOrNull() ?: 0.0
                    
                    // Calcular factor localmente para mostrar inmediatamente
                    val factor = if (peso > 0) metros / peso else 0.0
                    binding.tvFactor.text = MathUtils.formatearDecimal(factor)
                    
                    // Notificar cambio al ViewModel
                    onRegistroChanged(position, peso, metros)
                }
            }

            metrosWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val texto = s?.toString() ?: ""
                    if (texto.isEmpty()) {
                        // Si está vacío, mostrar 0.0000 pero no notificar aún
                        binding.tvFactor.text = "0.0000"
                        onRegistroChanged(position, binding.etPesoBruto.text.toString().toDoubleOrNull() ?: 0.0, 0.0)
                        return
                    }
                    
                    val peso = binding.etPesoBruto.text.toString().toDoubleOrNull() ?: 0.0
                    val metros = texto.toDoubleOrNull() ?: 0.0
                    
                    // Calcular factor localmente para mostrar inmediatamente
                    val factor = if (peso > 0) metros / peso else 0.0
                    binding.tvFactor.text = MathUtils.formatearDecimal(factor)
                    
                    // Notificar cambio al ViewModel
                    onRegistroChanged(position, peso, metros)
                }
            }

            binding.etPesoBruto.addTextChangedListener(pesoWatcher)
            binding.etMetros.addTextChangedListener(metrosWatcher)

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
     */
    fun actualizarRegistros(nuevaLista: List<RegistroData>) {
        // Guardar la lista anterior para comparar
        val listaAnterior = registros.toList()
        
        // Solo actualizar si la lista realmente cambió
        if (listaAnterior.size != nuevaLista.size || listaAnterior != nuevaLista) {
            val oldSize = listaAnterior.size
            registros.clear()
            registros.addAll(nuevaLista)
            
            // Usar notifyItemChanged para evitar perder el foco en los campos
            if (oldSize == nuevaLista.size) {
                // Si el tamaño es igual, solo notificar cambios en items específicos
                for (i in nuevaLista.indices) {
                    if (i < oldSize && listaAnterior[i] != nuevaLista[i]) {
                        notifyItemChanged(i)
                    }
                }
            } else {
                // Si el tamaño cambió, usar notifyDataSetChanged
                notifyDataSetChanged()
            }
        }
    }
}
