package com.truckershub.features.home

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Calendar

@Composable
fun OsmMap(
    modifier: Modifier = Modifier
) {
    // Zeit-Check für Night Mode
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isNightMode = currentHour >= 17 || currentHour < 7
    // val isNightMode = true // <-- Hier wieder auskommentieren, wenn du den echten Modus willst!

    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            )

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                // Startposition (Mitte DE) - Falls kein GPS Signal da ist
                controller.setZoom(15.0) // Etwas näher ran
                controller.setCenter(GeoPoint(51.1657, 10.4515))

                // --- NEU: Standort-Anzeige (Der blaue Pfeil) ---
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                locationOverlay.enableMyLocation() // GPS aktivieren
                locationOverlay.enableFollowLocation() // Der Karte sagen: "Lauf dem Pfeil hinterher"
                this.overlays.add(locationOverlay)
                // ---------------------------------------------

                if (isNightMode) {
                    // Unser genialer Thub-Style Filter
                    val grayScaleMatrix = ColorMatrix().apply { setSaturation(0f) }
                    val inverseMatrix = ColorMatrix(
                        floatArrayOf(
                            -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                            0.0f, -1.0f, 0.0f, 0.0f, 255f,
                            0.0f, 0.0f, -1.0f, 0.0f, 255f,
                            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                        )
                    )
                    val thubTintMatrix = ColorMatrix(
                        floatArrayOf(
                            0.7f, 0f, 0f, 0f, 0f,
                            0f, 1.0f, 0f, 0f, 0f,
                            0f, 0f, 1.3f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    val finalMatrix = ColorMatrix()
                    finalMatrix.postConcat(grayScaleMatrix)
                    finalMatrix.postConcat(inverseMatrix)
                    finalMatrix.postConcat(thubTintMatrix)

                    val filter = ColorMatrixColorFilter(finalMatrix)
                    this.overlayManager.tilesOverlay.setColorFilter(filter)
                }
            }
        }
    )
}