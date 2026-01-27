package com.skyrunner.md

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.skyrunner.md.adapter.HistorialAdapter
import com.skyrunner.md.adapter.RegistroAdapter
import com.skyrunner.md.databinding.ActivityMainBinding
import com.skyrunner.md.utils.HistorialHelper
import com.skyrunner.md.viewmodel.FactorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

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

    // Contract para seleccionar archivo CSV
    private val seleccionarArchivoCSV = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importarCSV(it)
        }
    }

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

        binding.btnLimpiarOutliers.setOnClickListener {
            viewModel.limpiarOutliers()
            Toast.makeText(this, "Todos los valores han sido limpiados", Toast.LENGTH_SHORT).show()
        }

        binding.btnImportarCSV.setOnClickListener {
            seleccionarArchivoCSV.launch("*/*")
        }

        binding.btnEliminarFilasVacias.setOnClickListener {
            val cantidadAntes = viewModel.registros.value?.size ?: 0
            viewModel.eliminarFilasVacias()
            val cantidadDespues = viewModel.registros.value?.size ?: 0
            val eliminadas = cantidadAntes - cantidadDespues
            
            if (eliminadas > 0) {
                Toast.makeText(
                    this,
                    getString(R.string.filas_vacias_eliminadas, eliminadas),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.sin_filas_vacias),
                    Toast.LENGTH_SHORT
                ).show()
            }
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

    /**
     * Importa datos desde un archivo CSV
     * @param uri URI del archivo CSV seleccionado
     */
    private fun importarCSV(uri: Uri) {
        lifecycleScope.launch {
            try {
                val contenido = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    } ?: ""
                }

                if (contenido.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.formato_csv_invalido),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val registrosImportados = viewModel.importarDesdeCSV(contenido)

                if (registrosImportados > 0) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.exito_importar_csv, registrosImportados),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.formato_csv_invalido),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.error_importar_csv, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
