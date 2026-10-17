package com.example.tomarguardarfoto

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BaseDeDatos(contexto: Context) : SQLiteOpenHelper(contexto, "GaleriaDB", null, 1){

    override fun onCreate(db: SQLiteDatabase?) {
        val crearTabla = """
            CREATE TABLE fotos (
                codigoFoto INTEGER PRIMARY KEY AUTOINCREMENT,
                ubicacionFoto TEXT,
                fechaTomaRegistro TEXT)
                """.trimIndent()//Funciona para quitar los espacios invisibles por ser texto de varias líneas

        //Ejecutamos el crear la tabla
        db?.execSQL(crearTabla)
    }

    //Es por si se actualiza la app y necesito botar la tabla vieja y poner una nueva
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}