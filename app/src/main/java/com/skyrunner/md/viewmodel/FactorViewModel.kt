package com.skyrunner.md.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.skyrunner.md.data.RegistroData
import com.skyrunner.md.data.ResultadoCalculo
import com.skyrunner.md.utils.MathUtils

/**
 * ViewModel para gestionar la lógica de negocio del cálculo de factores
 * Implementa arquitectura MVVM
 */
class FactorViewModel : ViewModel() {

    // LiveData para la lista de registros
    private val _registros = MutableLiveData<List<RegistroData>>()
    val registros: LiveData<List<RegistroData>> = _registros

    // LiveData para el resultado del cálculo
    private val _resultado = MutableLiveData<ResultadoCalculo?>()
    val resultado: LiveData<ResultadoCalculo?> = _resultado

    // LiveData para mensajes de error
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        // Inicializar con una fila vacía
        _registros.value = listOf(RegistroData())
    }

    /**
     * Agrega una nueva fila vacía a la lista de registros
     */
    fun agregarFila() {
        val listaActual = _registros.value?.toMutableList() ?: mutableListOf()
        listaActual.add(RegistroData())
        _registros.value = listaActual
    }

    /**
     * Actualiza un registro en la lista
     * @param posicion Posición del registro en la lista
     * @param pesoBruto Nuevo valor de peso bruto
     * @param metros Nuevo valor de metros
     */
    fun actualizarRegistro(posicion: Int, pesoBruto: Double, metros: Double) {
        val listaActual = _registros.value?.toMutableList() ?: return
        
        if (posicion in listaActual.indices) {
            val registroActual = listaActual[posicion]
            // Solo actualizar si los valores han cambiado para evitar loops infinitos
            if (registroActual.pesoBruto != pesoBruto || registroActual.metros != metros) {
                val factor = if (pesoBruto > 0) metros / pesoBruto else 0.0
                val registro = registroActual.copy(
                    pesoBruto = pesoBruto,
                    metros = metros,
                    factor = factor
                )
                listaActual[posicion] = registro
                _registros.value = listaActual
            }
        }
    }

    /**
     * Elimina un registro de la lista
     * @param posicion Posición del registro a eliminar
     */
    fun eliminarRegistro(posicion: Int) {
        val listaActual = _registros.value?.toMutableList() ?: return
        
        if (posicion in listaActual.indices) {
            listaActual.removeAt(posicion)
            // Si la lista queda vacía, agregar una fila vacía
            if (listaActual.isEmpty()) {
                listaActual.add(RegistroData())
            }
            _registros.value = listaActual
        }
    }

    /**
     * Calcula el factor promedio y estadísticas del grupo
     * Valida que haya al menos un registro válido
     */
    fun calcularFactorGrupo() {
        val listaActual = _registros.value ?: return
        
        // Filtrar solo registros válidos (peso > 0)
        val registrosValidos = listaActual.filter { it.esValido() }
        
        if (registrosValidos.isEmpty()) {
            _errorMessage.value = "Debe haber al menos un registro válido (Peso Bruto > 0)"
            return
        }

        // Calcular factores para registros válidos
        val factores = registrosValidos.map { it.calcularFactor() }
        
        // Calcular promedio
        val promedio = MathUtils.calcularPromedio(factores)
        
        // Calcular desviación estándar
        val desviacionEstandar = MathUtils.calcularDesviacionEstandar(factores, promedio)
        
        // Calcular margen de error
        val margenError = MathUtils.calcularMargenError(desviacionEstandar, promedio)
        
        // Detectar outliers
        val registrosConOutliers = MathUtils.detectarOutliers(
            registrosValidos,
            promedio,
            desviacionEstandar
        )
        
        // Actualizar la lista con los outliers marcados
        val listaActualizada = listaActual.map { registro ->
            val registroActualizado = registrosConOutliers.find { 
                it.pesoBruto == registro.pesoBruto && it.metros == registro.metros 
            }
            registroActualizado ?: registro
        }
        _registros.value = listaActualizada
        
        // Crear resultado
        val resultado = ResultadoCalculo(
            factorPromedio = promedio,
            desviacionEstandar = desviacionEstandar,
            margenError = margenError,
            totalRegistros = registrosValidos.size,
            registros = listaActualizada
        )
        
        _resultado.value = resultado
        _errorMessage.value = null
    }

    /**
     * Limpia los valores atípicos (outliers) de la lista
     * Limpia todos los valores de todas las filas, dejándolas vacías
     */
    fun limpiarOutliers() {
        val listaActual = _registros.value ?: return
        
        // Limpiar todos los valores de todas las filas, pero mantener las filas
        val listaLimpia = listaActual.map { 
            RegistroData(
                pesoBruto = 0.0,
                metros = 0.0,
                factor = 0.0,
                isOutlier = false
            )
        }
        
        // Si la lista está vacía, agregar una fila vacía
        if (listaLimpia.isEmpty()) {
            _registros.value = listOf(RegistroData())
        } else {
            _registros.value = listaLimpia
        }
        
        // Limpiar resultado anterior
        _resultado.value = null
    }

    /**
     * Limpia todos los registros y reinicia el estado
     */
    fun limpiarTodo() {
        _registros.value = listOf(RegistroData())
        _resultado.value = null
        _errorMessage.value = null
    }

    /**
     * Elimina todas las filas vacías (sin valores)
     * Mantiene al menos una fila vacía si todas están vacías
     */
    fun eliminarFilasVacias() {
        val listaActual = _registros.value ?: return
        val listaFiltrada = listaActual.filter { 
            it.pesoBruto > 0 || it.metros > 0 
        }
        
        // Si todas las filas estaban vacías, mantener una fila vacía
        if (listaFiltrada.isEmpty()) {
            _registros.value = listOf(RegistroData())
        } else {
            _registros.value = listaFiltrada
        }
        
        // Limpiar resultado anterior si se eliminaron filas
        if (listaFiltrada.size < listaActual.size) {
            _resultado.value = null
        }
    }

    /**
     * Convierte peso a metros usando el factor promedio
     * @param peso Peso a convertir
     * @return Metros calculados
     */
    fun convertirPesoAMetros(peso: Double): Double {
        val factorPromedio = _resultado.value?.factorPromedio ?: return 0.0
        return if (peso > 0) {
            peso * factorPromedio
        } else {
            0.0
        }
    }

    /**
     * Importa registros desde un archivo CSV
     * El formato esperado es: peso,metros (una línea por registro)
     * @param contenidoCSV Contenido del archivo CSV como String
     * @return Número de registros importados exitosamente
     */
    fun importarDesdeCSV(contenidoCSV: String): Int {
        val lineas = contenidoCSV.split("\n")
        val nuevosRegistros = mutableListOf<RegistroData>()
        var registrosImportados = 0

        for (linea in lineas) {
            val lineaLimpia = linea.trim()
            if (lineaLimpia.isEmpty()) continue

            // Dividir por coma o punto y coma
            val columnas = lineaLimpia.split(",", ";")
            
            if (columnas.size >= 2) {
                val peso = columnas[0].trim().toDoubleOrNull()
                val metros = columnas[1].trim().toDoubleOrNull()

                if (peso != null && metros != null && peso > 0) {
                    val factor = metros / peso
                    nuevosRegistros.add(
                        RegistroData(
                            pesoBruto = peso,
                            metros = metros,
                            factor = factor
                        )
                    )
                    registrosImportados++
                }
            }
        }

        if (nuevosRegistros.isNotEmpty()) {
            val listaActual = _registros.value?.toMutableList() ?: mutableListOf()
            // Agregar los nuevos registros a la lista existente
            listaActual.addAll(nuevosRegistros)
            _registros.value = listaActual
        }

        return registrosImportados
    }
}
