package com.skyrunner.md

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.skyrunner.md.adapter.HistorialAdapter
import com.skyrunner.md.adapter.RegistroAdapter
import com.skyrunner.md.databinding.ActivityMainBinding
import com.skyrunner.md.utils.HistorialHelper
import com.skyrunner.md.viewmodel.FactorViewModel
import kotlinx.coroutines.launch

/**
 * MainActivity - Pantalla principal de la aplicación
 * Permite ingresar múltiples registros de Peso Bruto y Metros
 * Calcula el factor individual y del grupo
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: FactorViewModel by viewModels()
    private lateinit var registroAdapter: RegistroAdapter
    private lateinit var historialAdapter: HistorialAdapter
    private lateinit var historialHelper: HistorialHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)

        // Inicializar helper de historial
        historialHelper = HistorialHelper(this)

        // Configurar RecyclerView de registros
        setupRegistrosRecyclerView()

        // Configurar RecyclerView de historial
        setupHistorialRecyclerView()

        // Configurar listeners
        setupListeners()

        // Observar cambios en el ViewModel
        observeViewModel()
    }

    /**
     * Configura el RecyclerView para los registros
     */
    private fun setupRegistrosRecyclerView() {
        registroAdapter = RegistroAdapter(
            registros = mutableListOf(),
            onRegistroChanged = { posicion, peso, metros ->
                viewModel.actualizarRegistro(posicion, peso, metros)
            },
            onRegistroDeleted = { posicion ->
                viewModel.eliminarRegistro(posicion)
            }
        )

        binding.recyclerViewRegistros.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = registroAdapter
        }
    }

    /**
     * Configura el RecyclerView para el historial
     */
    private fun setupHistorialRecyclerView() {
        historialAdapter = HistorialAdapter(
            historial = emptyList(),
            historialHelper = historialHelper
        )

        binding.recyclerViewHistorial.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historialAdapter
        }
    }

    /**
     * Configura los listeners de los botones
     */
    private fun setupListeners() {
        binding.btnAgregarFila.setOnClickListener {
            viewModel.agregarFila()
        }

        binding.btnCalcular.setOnClickListener {
            viewModel.calcularFactorGrupo()
        }
    }

    /**
     * Observa los cambios en el ViewModel
     */
    private fun observeViewModel() {
        // Observar cambios en la lista de registros
        viewModel.registros.observe(this) { registros ->
            registroAdapter.actualizarRegistros(registros)
        }

        // Observar resultado del cálculo
        viewModel.resultado.observe(this) { resultado ->
            resultado?.let {
                // Guardar en historial
                historialHelper.guardarCalculo(it.factorPromedio)
                
                // Actualizar historial en pantalla
                actualizarHistorial()
                
                // Navegar a la pantalla de resultados
                val intent = Intent(this, ResultadoActivity::class.java).apply {
                    putExtra("factor_promedio", it.factorPromedio)
                    putExtra("margen_error", it.margenError)
                    putExtra("total_registros", it.totalRegistros)
                    putExtra("desviacion_estandar", it.desviacionEstandar)
                }
                startActivity(intent)
            }
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(this) { mensaje ->
            mensaje?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Actualiza el historial en la pantalla
     */
    private fun actualizarHistorial() {
        lifecycleScope.launch {
            val historial = historialHelper.obtenerHistorial()
            historialAdapter = HistorialAdapter(historial, historialHelper)
            binding.recyclerViewHistorial.adapter = historialAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar historial cuando se vuelve a la pantalla
        actualizarHistorial()
    }
}
