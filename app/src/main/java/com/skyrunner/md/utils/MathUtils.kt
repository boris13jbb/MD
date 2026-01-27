package com.skyrunner.md.utils

import com.skyrunner.md.data.RegistroData

/**
 * Utilidades matemáticas para cálculos estadísticos
 */
object MathUtils {

    /**
     * Calcula el promedio de una lista de valores
     * @param valores Lista de valores Double
     * @return El promedio o 0.0 si la lista está vacía
     */
    fun calcularPromedio(valores: List<Double>): Double {
        if (valores.isEmpty()) return 0.0
        return valores.sum() / valores.size
    }

    /**
     * Calcula la desviación estándar de una lista de valores
     * @param valores Lista de valores Double
     * @param promedio El promedio de los valores (para optimización)
     * @return La desviación estándar o 0.0 si la lista está vacía
     */
    fun calcularDesviacionEstandar(valores: List<Double>, promedio: Double): Double {
        if (valores.isEmpty()) return 0.0
        
        val sumaDiferenciasCuadradas = valores.sumOf { (it - promedio) * (it - promedio) }
        val varianza = sumaDiferenciasCuadradas / valores.size
        return kotlin.math.sqrt(varianza)
    }

    /**
     * Calcula el margen de error como porcentaje
     * @param desviacionEstandar Desviación estándar
     * @param promedio Promedio de los valores
     * @return Margen de error en porcentaje o 0.0 si el promedio es 0
     */
    fun calcularMargenError(desviacionEstandar: Double, promedio: Double): Double {
        return if (promedio != 0.0) {
            (desviacionEstandar / promedio) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Detecta valores atípicos (outliers) en una lista de registros
     * Un valor es atípico si está fuera de ±2 desviaciones estándar del promedio
     * @param registros Lista de registros a analizar
     * @param promedio Promedio de los factores
     * @param desviacionEstandar Desviación estándar de los factores
     * @return Lista de registros actualizada con la marca de outlier
     */
    fun detectarOutliers(
        registros: List<RegistroData>,
        promedio: Double,
        desviacionEstandar: Double
    ): List<RegistroData> {
        val limiteSuperior = promedio + (2 * desviacionEstandar)
        val limiteInferior = promedio - (2 * desviacionEstandar)

        return registros.map { registro ->
            val esOutlier = registro.factor < limiteInferior || registro.factor > limiteSuperior
            registro.copy(isOutlier = esOutlier)
        }
    }

    /**
     * Formatea un número Double a String con 4 decimales
     * @param valor Valor a formatear
     * @return String formateado
     */
    fun formatearDecimal(valor: Double): String {
        return String.format("%.4f", valor)
    }

    /**
     * Formatea un porcentaje con 2 decimales
     * @param valor Valor del porcentaje
     * @return String formateado con símbolo %
     */
    fun formatearPorcentaje(valor: Double): String {
        return String.format("%.2f%%", valor)
    }
}
