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
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row

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

    // Contract para seleccionar archivo Excel
    private val seleccionarArchivoExcel = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importarExcel(it)
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

        binding.btnImportarExcel.setOnClickListener {
            seleccionarArchivoExcel.launch("*/*")
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

        binding.btnBorrarHistorial.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.borrar_historial))
                .setMessage(getString(R.string.confirmar_borrar_historial))
                .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                    historialHelper.limpiarHistorial()
                    actualizarHistorial()
                    Toast.makeText(
                        this,
                        getString(R.string.historial_borrado),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(getString(android.R.string.cancel), null)
                .show()
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

    /**
     * Importa datos desde un archivo Excel (.xls, .xlsx)
     * @param uri URI del archivo Excel seleccionado
     */
    private fun importarExcel(uri: Uri) {
        lifecycleScope.launch {
            try {
                val registrosImportados = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val workbook = WorkbookFactory.create(inputStream)
                        val sheet: Sheet = workbook.getSheetAt(0) // Primera hoja
                        val registros = mutableListOf<Pair<Double, Double>>()
                        
                        // Leer desde la segunda fila (índice 1) para saltar el encabezado
                        for (rowIndex in 1 until sheet.lastRowNum + 1) {
                            val row: Row? = sheet.getRow(rowIndex)
                            if (row != null && row.lastCellNum >= 2) {
                                val peso = row.getCell(0)?.numericCellValue ?: 0.0
                                val metros = row.getCell(1)?.numericCellValue ?: 0.0
                                
                                if (peso > 0 && metros > 0) {
                                    registros.add(Pair(peso, metros))
                                }
                            }
                        }
                        
                        workbook.close()
                        registros
                    } ?: emptyList()
                }

                if (registrosImportados.isNotEmpty()) {
                    // Importar los registros al ViewModel
                    val contenidoCSV = registrosImportados.joinToString("\n") { "${it.first},${it.second}" }
                    val cantidad = viewModel.importarDesdeCSV(contenidoCSV)
                    
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.exito_importar_excel, cantidad),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.formato_excel_invalido),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.error_importar_excel, e.message ?: "Error desconocido"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
