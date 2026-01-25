package com.truckershub.features.parking.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.truckershub.core.data.model.AmpelStatus
import com.truckershub.core.data.model.ParkingSpot
import com.truckershub.core.data.model.ParkingType
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * PARKPLATZ-MARKER HELPER (PRO VERSION 🎨)
 * * Zeigt Typ (Farbe/Buchstabe) UND Status (Ring) gleichzeitig an.
 */
object ParkingMarkerHelper {

    /**
     * Hauptfunktion: Aktualisiert alle Marker auf der Karte
     */
    fun updateParkingMarkers(
        mapView: MapView,
        parkings: List<ParkingSpot>,
        onParkingClick: (ParkingSpot) -> Unit
    ) {
        // 1. Alte Parkplatz-Marker entfernen
        // Wir nutzen eine ID-Prüfung, um User/Route Marker nicht zu löschen
        mapView.overlays.removeIf { overlay ->
            overlay is Marker && overlay.id.startsWith("parking_")
        }

        // 2. Neue Marker setzen
        parkings.forEach { parking ->
            val marker = Marker(mapView)
            marker.id = "parking_${parking.id}" // Eindeutige ID
            marker.position = OsmGeoPoint(parking.location.latitude, parking.location.longitude)
            marker.title = parking.name
            marker.snippet = "${parking.type.name} - ${getAmpelText(parking.currentAmpel)}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Das dynamische Icon generieren
            val icon = createParkingIcon(mapView.context, parking)
            marker.icon = icon

            marker.setOnMarkerClickListener { _, _ ->
                onParkingClick(parking)
                true // Event konsumiert
            }

            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    /**
     * Der "Maler": Zeichnet das Icon dynamisch
     */
    private fun createParkingIcon(context: Context, parking: ParkingSpot): Drawable {
        val size = 110 // Größe des Icons
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // 1. BASIS-FARBE (Je nach Typ)
        val baseColor = when (parking.type) {
            ParkingType.AUTOHOF -> Color.parseColor("#9C27B0")    // Lila
            ParkingType.RASTSTAETTE -> Color.parseColor("#FF9800") // Orange
            ParkingType.PARKPLATZ -> Color.parseColor("#2196F3")   // Blau
            ParkingType.INDUSTRIEGEBIET -> Color.DKGRAY            // Dunkelgrau
            else -> Color.GRAY
        }

        // 2. KREIS ZEICHNEN (Hintergrund)
        paint.style = Paint.Style.FILL
        paint.color = baseColor
        // Wir lassen etwas Platz am Rand für den Ampel-Ring (size/2 - 12)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 12, paint)

        // 3. AMPEL-STATUS ALS RING (Rahmen)
        val statusColor = when (parking.currentAmpel) {
            AmpelStatus.GREEN -> Color.parseColor("#00C853")  // Sattes Grün
            AmpelStatus.YELLOW -> Color.parseColor("#FFD600") // Sattes Gelb
            AmpelStatus.RED -> Color.parseColor("#D50000")    // Sattes Rot
            else -> Color.WHITE // Weißer Ring wenn unbekannt
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f // Dicke des Rings
        paint.color = statusColor
        // Der Ring liegt genau um den Kreis herum
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, paint)

        // 4. BUCHSTABE (A, R, P)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER

        val letter = when(parking.type) {
            ParkingType.AUTOHOF -> "A"
            ParkingType.RASTSTAETTE -> "R"
            ParkingType.INDUSTRIEGEBIET -> "I"
            else -> "P" // Parkplatz oder Unbekannt
        }

        // Text vertikal zentrieren
        val bounds = Rect()
        paint.getTextBounds(letter, 0, letter.length, bounds)
        val yOffset = bounds.height() / 2f
        canvas.drawText(letter, size / 2f, size / 2f + yOffset, paint)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun getAmpelText(status: AmpelStatus): String {
        return when (status) {
            AmpelStatus.GREEN -> "Viel Platz"
            AmpelStatus.YELLOW -> "Wird voll"
            AmpelStatus.RED -> "Alles voll"
            AmpelStatus.UNKNOWN -> "Keine Info"
        }
    }
}

/**
 * DISTANZ-HELPER (Bleibt erhalten!)
 */
object DistanceHelper {

    fun getDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 10.0 -> String.format("%.1f km", distanceKm)
            else -> "${distanceKm.toInt()} km"
        }
    }
}