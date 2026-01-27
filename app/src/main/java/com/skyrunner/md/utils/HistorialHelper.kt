package com.skyrunner.md.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skyrunner.md.data.HistorialCalculo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper para gestionar el historial de cálculos usando SharedPreferences
 */
class HistorialHelper(private val context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "factor_calculator_prefs"
        private const val KEY_HISTORIAL = "historial_calculos"
        private const val MAX_HISTORIAL = 5
    }

    /**
     * Guarda un nuevo cálculo en el historial
     * Mantiene solo los últimos MAX_HISTORIAL registros
     * @param factorPromedio Factor promedio a guardar
     */
    fun guardarCalculo(factorPromedio: Double) {
        val historial = obtenerHistorial().toMutableList()
        historial.add(0, HistorialCalculo(factorPromedio)) // Agregar al inicio
        
        // Mantener solo los últimos MAX_HISTORIAL
        if (historial.size > MAX_HISTORIAL) {
            historial.removeAt(historial.size - 1)
        }
        
        val json = gson.toJson(historial)
        prefs.edit().putString(KEY_HISTORIAL, json).apply()
    }

    /**
     * Obtiene el historial de cálculos
     * @return Lista de HistorialCalculo ordenada por fecha (más reciente primero)
     */
    fun obtenerHistorial(): List<HistorialCalculo> {
        val json = prefs.getString(KEY_HISTORIAL, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<HistorialCalculo>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Limpia todo el historial
     */
    fun limpiarHistorial() {
        prefs.edit().remove(KEY_HISTORIAL).apply()
    }

    /**
     * Formatea la fecha de un cálculo para mostrar
     * @param timestamp Timestamp en milisegundos
     * @return String formateado con fecha y hora
     */
    fun formatearFecha(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
