package com.skyrunner.md

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.skyrunner.md.adapter.RegistroAdapter
import com.skyrunner.md.databinding.ActivityResultadoBinding
import com.skyrunner.md.utils.MathUtils
import com.skyrunner.md.viewmodel.FactorViewModel
import androidx.activity.viewModels

/**
 * ResultadoActivity - Muestra los resultados del cálculo
 * Incluye factor promedio, margen de error, total de registros
 * Permite limpiar valores atípicos
 */
class ResultadoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultadoBinding
    private val viewModel: FactorViewModel by viewModels()
    private lateinit var registroAdapter: RegistroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Obtener datos del intent
        val factorPromedio = intent.getDoubleExtra("factor_promedio", 0.0)
        val margenError = intent.getDoubleExtra("margen_error", 0.0)
        val totalRegistros = intent.getIntExtra("total_registros", 0)
        val desviacionEstandar = intent.getDoubleExtra("desviacion_estandar", 0.0)

        // Mostrar resultados
        mostrarResultados(factorPromedio, margenError, totalRegistros)

        // Configurar RecyclerView
        setupRegistrosRecyclerView()

        // Observar cambios en los registros para mostrar outliers
        viewModel.registros.observe(this) { registros ->
            registroAdapter.actualizarRegistros(registros)
        }

        // Configurar botón limpiar outliers
        binding.btnLimpiarOutliers.setOnClickListener {
            viewModel.limpiarOutliers()
            // Volver a MainActivity después de limpiar
            finish()
        }
    }

    /**
     * Muestra los resultados en la interfaz
     */
    private fun mostrarResultados(
        factorPromedio: Double,
        margenError: Double,
        totalRegistros: Int
    ) {
        binding.tvFactorPromedio.text = MathUtils.formatearDecimal(factorPromedio)
        binding.tvMargenError.text = MathUtils.formatearPorcentaje(margenError)
        binding.tvTotalRegistros.text = totalRegistros.toString()
    }

    /**
     * Configura el RecyclerView para mostrar los registros
     */
    private fun setupRegistrosRecyclerView() {
        registroAdapter = RegistroAdapter(
            registros = mutableListOf(),
            onRegistroChanged = { _, _, _ -> },
            onRegistroDeleted = { }
        )

        binding.recyclerViewRegistros.apply {
            layoutManager = LinearLayoutManager(this@ResultadoActivity)
            adapter = registroAdapter
        }

        // Cargar registros actuales del ViewModel
        viewModel.registros.value?.let {
            registroAdapter.actualizarRegistros(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
