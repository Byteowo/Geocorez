package com.example.app1

// --- Imports Android/Kotlin ---
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

// --- Imports Firebase ---
import com.google.firebase.database.FirebaseDatabase

// --- Imports Mapbox ---
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CODIGO_PERMISO_UBICACION = 1000
    }

    private lateinit var mapaMapBox: MapView
    private var gestorMarcadores: PointAnnotationManager? = null

    private var ubiActual: Point? = null
    private var escuchaUbicacion: OnIndicatorPositionChangedListener? = null

    private lateinit var barraDeLatYLon: TextView

    private var marcadorUsuario: PointAnnotation? = null
    private var ubiAnterior: Point? = null

    // Referencia a Firebase para la base de datos en tiempo real
    private val dbRef = FirebaseDatabase.getInstance().getReference("ubicacion_usuario")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mapaMapBox = findViewById(R.id.mapView)

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

    private fun actualizarMarcador(punto: Point) {
        runOnUiThread {
            val colorMarcador = determinarColorMarcador(punto)
            if (marcadorUsuario == null) {
                marcadorUsuario = gestorMarcadores?.create(
                    PointAnnotationOptions()
                        .withPoint(punto)
                        .withIconImage(crearMarcadorColor(colorMarcador))
                )
            } else {
                marcadorUsuario?.point = punto
                marcadorUsuario?.iconImageBitmap = crearMarcadorColor(colorMarcador)
                gestorMarcadores?.update(marcadorUsuario!!)
            }
            barraDeLatYLon.text = "Lat: %.5f, Lon: %.5f".format(punto.latitude(), punto.longitude())

            // Envía la ubicación a Firebase en tiempo real
            val datosUbicacion = mapOf("lat" to punto.latitude(), "lon" to punto.longitude())
            dbRef.setValue(datosUbicacion)

            val options = com.mapbox.maps.CameraOptions.Builder()
                .center(punto)
                .zoom(15.0)
                .build()
            mapaMapBox.getMapboxMap().setCamera(options)
            ubiAnterior = punto
        }
    }

    private fun determinarColorMarcador(punto: Point): Int {
        return 0xFF0000FF.toInt()
    }

    private fun crearMarcadorColor(colorHex: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = colorHex }
        canvas.drawCircle(20f, 20f, 20f, paint)
        return bitmap
    }
}