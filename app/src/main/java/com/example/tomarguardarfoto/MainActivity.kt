package com.example.tomarguardarfoto

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
//import java.util.jar.Manifest
import android.Manifest
import android.app.Activity
import android.app.ComponentCaller
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//Para trabajar con la camara es necesario pedirle permiso al telefono
const val CODIGO_PERMISO_CAMARA = 100
const val CODIGO_SERVICIO_CAM = 101
const val CODIGO_SERVICIO_BLUETOOTH = 300

class MainActivity : AppCompatActivity() {
    //Creamos el objeto de la interfaz grafica del LinearLayout
    lateinit var cajaFotos: LinearLayout
    //Creamos la variable que tendra la ruta de la fotografia
    var ubicacionFoto: String? = null

    //Preparamos la conexion a la base de datos
    private lateinit var conexionBD: BaseDeDatos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //Enlazamos el objeto cajaFotos a la interfaz grafica
        cajaFotos = findViewById(R.id.cajaFotosGuardar)
        //Inicia la conexion a la base de datos
        conexionBD = BaseDeDatos(this)
    }
    /*Creamos la funcion para tomar la fotografia, pero primero se deben validar los permisos
    del android manifest*/
    fun tomarFoto(V: View){
        //Validamos los permisos de camara con if
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED){
            capturarFoto()
        }else{
            //Si el usuario no ha dado permisos, en else se los solicitamos
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                CODIGO_PERMISO_CAMARA)
        }
    }

    private fun crearArchivo(): File {
        //Generamos el formato que tendra el nombre del archivo
        val instanteActual = SimpleDateFormat("yyyyMMdd_Hh-mm-ss",
            Locale.getDefault()).format(Date())
        //Preparamos el nombre del archivo
        val nombreArchivo = "IMG_" + instanteActual
        //Generamos la carpeta de imagenes en el
        val nuevaUbicacion = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //Creando el archivo vacio
        val imagenTemporal = File.createTempFile(
            nombreArchivo, ".jpg", nuevaUbicacion
        )
        //El archivo vacio ya tiene una ruta, asignamos esa ruta a nuestra variable ubicacionFoto
        ubicacionFoto = imagenTemporal.absolutePath
        return imagenTemporal
    }

    //Creamos el metodo para capturar la foto
    fun capturarFoto(){
        /*Activamos el servicio de camara a traves del intent, el intent nos permite cambiar entre
        pantallas y en esta ocasion lo usamos para activar el servicio de camara*/
        val activarServicioCamara = Intent(MediaStore.ACTION_IMAGE_CAPTURE) //Cuando se active esta linea la app se pone en pausa
        //Con un if validaremos cuando el servicio haya terminado y se activa de nuevo la app
        if (activarServicioCamara.resolveActivity(packageManager)!=null) { //evaluamos que no sea null y si haya tomado una foto
            var archivoFoto: File?=null //Creamos un archivo vacio para nuestra foto
            try {
                archivoFoto = crearArchivo()
            }catch (e: IOException){
                e.printStackTrace()
            }

            //Validamos que el archivo vacio sea distinto de nulo
            if (archivoFoto != null){
                val fotoUri: Uri = FileProvider.getUriForFile(
                    this,
                    "com.example.tomarguardarfoto",
                    archivoFoto
                )
                //Combinamos el archivo vacio con nuestra foto de resultado
                activarServicioCamara.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
                //Esta línea reanuda nuestra app con un resultado
                startActivityForResult(activarServicioCamara, CODIGO_SERVICIO_CAM)
            }
        }
    }

    //Establecemos el constructor de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        //Este constructor presenta un modal al usuario para conceder permisos
        //Validamos que el usuario conceda los permisos con un if
        if (requestCode == CODIGO_PERMISO_CAMARA){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                capturarFoto()
            }else{
                Toast.makeText(this, "Necesita aceptar permisos", Toast.LENGTH_LONG).show()
            }
        }
    }

    //Constructor para reanudar nuestra app
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        //Validamos el codigo del servicio del cual estamos retornando y que todo haya sido exitoso
        if (requestCode == CODIGO_SERVICIO_CAM && resultCode == Activity.RESULT_OK){
            val nuevaImagen = ImageView(this)
            //Insertando imagen de la variable ubicacionFoto
            nuevaImagen.setImageURI(Uri.parse(ubicacionFoto))
            nuevaImagen.scaleType = ImageView.ScaleType.CENTER_CROP
            //Añadimos la nueva imagen al LinearLayout
            cajaFotos.addView(nuevaImagen)

            //----AQUI EMPIEZA LA BASE DE DATOS----

            //Abrimos el archivo en modo escritura
            val accionInsertar = conexionBD.writableDatabase

            //Sacamos la fecha y hora
            val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            //Empaquetamos los datos (ubicacion y fecha)
            val datosFoto = ContentValues()
            datosFoto.put("ubicacionFoto", ubicacionFoto)
            datosFoto.put("fechaTomaRegistro", fechaActual)

            //Mandamos el insert a la tabla "fotos"
            val resultado = accionInsertar.insert("fotos", null, datosFoto)
        }
    }
}