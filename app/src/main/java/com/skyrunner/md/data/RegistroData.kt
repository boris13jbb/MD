package com.skyrunner.md.data

/**
 * Data class que representa un registro con Peso Bruto y Metros
 * @param pesoBruto Peso bruto del registro (debe ser > 0)
 * @param metros Cantidad de metros
 * @param factor Factor calculado (Metros / PesoBruto)
 * @param isOutlier Indica si este registro es un valor atípico
 */
data class RegistroData(
    var pesoBruto: Double = 0.0,
    var metros: Double = 0.0,
    var factor: Double = 0.0,
    var isOutlier: Boolean = false
) {
    /**
     * Calcula el factor para este registro
     * @return El factor calculado o 0.0 si el peso es inválido
     */
    fun calcularFactor(): Double {
        return if (pesoBruto > 0) {
            metros / pesoBruto
        } else {
            0.0
        }
    }

    /**
     * Valida si el registro tiene datos válidos
     * @return true si el peso es mayor que 0
     */
    fun esValido(): Boolean {
        return pesoBruto > 0
    }
}

/**
 * Data class que representa el resultado del cálculo del grupo
 * @param factorPromedio Factor promedio de todos los registros válidos
 * @param desviacionEstandar Desviación estándar de los factores
 * @param margenError Margen de error en porcentaje
 * @param totalRegistros Total de registros válidos
 * @param registros Lista de todos los registros
 */
data class ResultadoCalculo(
    val factorPromedio: Double = 0.0,
    val desviacionEstandar: Double = 0.0,
    val margenError: Double = 0.0,
    val totalRegistros: Int = 0,
    val registros: List<RegistroData> = emptyList()
)

/**
 * Data class para el historial de cálculos
 * @param factorPromedio Factor promedio calculado
 * @param fecha Fecha del cálculo en formato timestamp
 */
data class HistorialCalculo(
    val factorPromedio: Double,
    val fecha: Long = System.currentTimeMillis()
)
