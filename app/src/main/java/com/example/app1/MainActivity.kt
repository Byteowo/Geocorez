package com.example.app1

// --- Imports Android/Kotlin ---
import android.Manifest // ACTIVA LOS PERMISOS DEL MANIFEST
import android.content.Intent // ENVIA LA UBI A OTRA APP (EN ESTE CASO A WHATSAPP)
import android.content.pm.PackageManager // COMPRUEBA LOS PERMISOS
import android.graphics.Bitmap // CREA EL ICONITO AZUL IGUAL AL DE GOOGLE MAPS
import android.graphics.Canvas // SE CREA DE FORMA FÍSICA EL ICONITO AZUL
import android.graphics.Paint // EL ICONITO SE PINTA DE COLOR
import android.os.Bundle // PROPIO DEL INTELLIJ
import android.widget.TextView // CUADRITO DONDE MUESTRA DATOS DE LATITUD Y LONGITUD
import androidx.activity.enableEdgeToEdge // PROPIO DEL INTELLIJ
import androidx.appcompat.app.AppCompatActivity // PROPIO DEL INTELLIJ
import androidx.core.app.ActivityCompat // Android: pedir permisos
import androidx.core.content.ContextCompat // Android: comprobar permisos
import androidx.core.view.ViewCompat // Android: manejar padding
import androidx.core.view.WindowInsetsCompat // Android: barras del sistema
import com.google.android.material.floatingactionbutton.FloatingActionButton // BOTONES FLOTANTES QUE EL INTELLIJ TIENE

// --- Imports Mapbox ---
import com.mapbox.geojson.Point // SE REPRESENTAN LAS COORDENADAS CON ESTO
import com.mapbox.maps.CameraOptions // CAMARA DEL MAPBOX
import com.mapbox.maps.MapView // PERMITE VER EL MAPA DEL MAPBOX
import com.mapbox.maps.Style // PERMITE CAMBIAR LOS ESTILOS DEL MAPITA DEL MAPBOX
import com.mapbox.maps.plugin.annotation.annotations // PUNTEROS DEL MAPBOX
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation // PUNTERO INDIVIDUAL DEL MAPBOX
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager // GESTIONA LOS PUNTEROS
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions // OPCIONES DE LOS PUNTEROS
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager // CREA PUNTEROS
import com.mapbox.maps.plugin.gestures.gestures // PERMITE HACER ZOOM O MOVERNOS POR EL MAPITA
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener // ESCUCHA O ATRAPA LA UBI
import com.mapbox.maps.plugin.locationcomponent.location // COMPONENTES DE LA UBI

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CODIGO_PERMISO_UBICACION = 1000
    }

    private lateinit var mapaMapBox: MapView
    private var gestorMarcadores: PointAnnotationManager? = null

    private var ubiActual: Point? = null
    private var escuchaUbicacion: OnIndicatorPositionChangedListener? = null

    private lateinit var barraDeLatYLon: TextView

    // Variables agregadas para la gestión del marcador y punto anterior
    private var marcadorUsuario: PointAnnotation? = null
    private var ubiAnterior: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mapaMapBox = findViewById(R.id.mapView)

        // //CUADRITO QUE MUESTRA LA LATITUD Y LONGITUD
        barraDeLatYLon = TextView(this).apply {
            textSize = 14f
            setPadding(20, 10, 20, 10)
            setBackgroundColor(0xFFBC5A94.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        addContentView(
            barraDeLatYLon,
            androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.WRAP_CONTENT
            ).apply {
                val altoBarraEstado = resources.getDimensionPixelSize(
                    resources.getIdentifier("status_bar_height", "dimen", "android")
                )
                topMargin = 120
                marginStart = 20
            }
        )

        mapaMapBox.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            gestorMarcadores = mapaMapBox.annotations.createPointAnnotationManager()
            habilitarUbicacion()
        }
    }

    private fun habilitarUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                CODIGO_PERMISO_UBICACION
            )
            return
        }

        mapaMapBox.location.apply {
            enabled = true
            pulsingEnabled = true
        }

        escuchaUbicacion = OnIndicatorPositionChangedListener { punto ->
            ubiActual = punto
            actualizarMarcador(punto)
        }
        mapaMapBox.location.addOnIndicatorPositionChangedListener(escuchaUbicacion!!)
    }

    // //ACTUALIZAR EL PUNTERITO SEGÚN LA UBI Y LA VELOCIDAD
    private fun actualizarMarcador(punto: Point) {
        runOnUiThread { // //PARA QUE NO SE MODIFIQUE EN SEGUNDO PLANO
            val colorMarcador = determinarColorMarcador(punto) // //LLAMO A LA FUNCIÓN QUE HACE QUE MI PUNTERO CAMBIE DE COLOR
            if (marcadorUsuario == null) { // //PUNTERO MARCANDO EN UN LUGAR
                marcadorUsuario = gestorMarcadores?.create(
                    PointAnnotationOptions()
                        .withPoint(punto) // Mapbox
                        .withIconImage(crearMarcadorColor(colorMarcador)) // Mapbox
                )
            } else { // Mapbox
                marcadorUsuario?.point = punto
                marcadorUsuario?.iconImageBitmap = crearMarcadorColor(colorMarcador)
                gestorMarcadores?.update(marcadorUsuario!!)
            }
            barraDeLatYLon.text = "Lat: %.5f, Lon: %.5f".format(punto.latitude(), punto.longitude()) // //PROPIO DEL IN
            // Mueve la cámara del mapa hacia la ubicación actual con zoom de calle
            val options = com.mapbox.maps.CameraOptions.Builder()
                .center(punto)
                .zoom(15.0)
                .build()
            mapaMapBox.getMapboxMap().setCamera(options)
            ubiAnterior = punto // Android
        }
    }

    // Métodos auxiliares temporales (se completarán en las siguientes diapositivas)
    private fun determinarColorMarcador(punto: Point): Int {
        return 0xFF0000FF.toInt() // Azul por defecto
    }

    private fun crearMarcadorColor(colorHex: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = colorHex }
        canvas.drawCircle(20f, 20f, 20f, paint)
        return bitmap
    }
}