package com.truckershub.features.parking.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.truckershub.R
import com.truckershub.core.data.model.AmpelStatus
import com.truckershub.core.data.model.ParkingSpot
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint as OsmGeoPoint

/**
 * PARKPLATZ-MARKER HELPER
 * 
 * Erstellt Marker für Parkplätze auf der Karte mit Ampel-Status
 */
object ParkingMarkerHelper {
    
    /**
     * Parkplatz-Marker zur Karte hinzufügen
     * 
     * @param mapView Die OSM-Karte
     * @param parking Der Parkplatz
     * @param onClick Was passiert beim Klick auf den Marker?
     */
    fun addParkingMarker(
        mapView: MapView,
        parking: ParkingSpot,
        onClick: (ParkingSpot) -> Unit
    ): Marker {
        val marker = Marker(mapView)
        marker.position = OsmGeoPoint(parking.location.latitude, parking.location.longitude)
        marker.title = parking.name
        marker.snippet = "${parking.type.name} - ${getAmpelText(parking.currentAmpel)}"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        
        // Klick-Listener
        marker.setOnMarkerClickListener { _, _ ->
            onClick(parking)
            true
        }
        
        // Icon je nach Ampel-Status
        val icon = getParkingIcon(mapView.context, parking.currentAmpel)
        marker.icon = icon
        
        return marker
    }
    
    /**
     * Icon für Parkplatz erstellen (mit Ampel-Farbe)
     */
    private fun getParkingIcon(context: Context, status: AmpelStatus): Drawable {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Ampel-Farbe bestimmen
        val ampelColor = when (status) {
            AmpelStatus.GREEN -> Color(0xFF00C853)
            AmpelStatus.YELLOW -> Color(0xFFFFA000)
            AmpelStatus.RED -> Color(0xFFD32F2F)
            AmpelStatus.UNKNOWN -> Color.Gray
        }
        
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Äußerer Kreis (Ampel-Ring)
        paint.color = ampelColor.toArgb()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // Innerer Kreis (Parkplatz-Symbol Hintergrund)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)
        
        // LKW-Icon in der Mitte (vereinfacht als Rechteck)
        paint.color = ampelColor.toArgb()
        val iconSize = size / 3f
        val left = (size - iconSize) / 2f
        val top = (size - iconSize) / 2f
        canvas.drawRect(left, top, left + iconSize, top + iconSize, paint)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    /**
     * Ampel-Status als Text
     */
    private fun getAmpelText(status: AmpelStatus): String {
        return when (status) {
            AmpelStatus.GREEN -> "Plätze frei"
            AmpelStatus.YELLOW -> "Wird voll"
            AmpelStatus.RED -> "Voll"
            AmpelStatus.UNKNOWN -> "Status unbekannt"
        }
    }
    
    /**
     * Alle Parkplatz-Marker aus der Karte entfernen
     */
    fun removeParkingMarkers(mapView: MapView) {
        mapView.overlays.removeIf { overlay ->
            overlay is Marker && overlay.id == "PARKING"
        }
    }
    
    /**
     * Parkplatz-Marker aktualisieren
     */
    fun updateParkingMarkers(
        mapView: MapView,
        parkings: List<ParkingSpot>,
        onParkingClick: (ParkingSpot) -> Unit
    ) {
        // Alte Marker entfernen
        removeParkingMarkers(mapView)
        
        // Neue Marker hinzufügen
        parkings.forEach { parking ->
            val marker = addParkingMarker(mapView, parking, onParkingClick)
            marker.id = "PARKING"
            mapView.overlays.add(marker)
        }
        
        mapView.invalidate()
    }
}

/**
 * DISTANZ-HELPER
 * 
 * Berechnet die Entfernung zwischen zwei Punkten
 */
object DistanceHelper {
    
    /**
     * Entfernung in Kilometern berechnen
     */
    fun getDistanceInKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Entfernung als formatierten String
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 10.0 -> String.format("%.1f km", distanceKm)
            else -> "${distanceKm.toInt()} km"
        }
    }
}
