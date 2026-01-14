package com.truckershub.features.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.truckershub.R
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Calendar

// Datenmodell
data class TruckerBuddy(
    val uid: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val status: String,
    val profileImageUrl: String,
    val truckType: String = "LKW"
)

@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userId = auth.currentUser?.uid

    // Zeit-Check für Night Mode
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isNightMode = currentHour >= 17 || currentHour < 7

    // Status
    var otherTrucks by remember { mutableStateOf<List<TruckerBuddy>>(emptyList()) }
    var selectedBuddy by remember { mutableStateOf<TruckerBuddy?>(null) } // Für den Klick-Dialog
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    var myLocationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }

    // --- DUMMY DATEN (JETZT IM NORDEN BEI DIR) ---
    val dummies = remember {
        listOf(
            // Einer steht direkt in Rodenkirchen am Markt
            TruckerBuddy("dummy1", 53.402, 8.448, "Diesel-Dieter", "Pause", "", "Sattelzug"),
            // Eine steht in Brake am Hafen
            TruckerBuddy("dummy2", 53.330, 8.480, "Logistik-Lisa", "Fahrbereit", "", "Tankwagen"),
            // Einer steht oben in Nordenham
            TruckerBuddy("dummy3", 53.485, 8.485, "Fernfahrer-Fritz", "Laden/Entl.", "", "Kipper")
        )
    }

    // 1. EMPFANGEN & FILTERN (70km Radius)
    LaunchedEffect(Unit) {
        firestore.collection("users").addSnapshotListener { snapshot, _ ->
            // Echte User + Dummies zusammenführen
            val combinedList = dummies.toMutableList()

            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    if (doc.id == userId) continue
                    val geo = doc.getGeoPoint("currentLocation")
                    val name = doc.getString("funkName") ?: doc.getString("firstName") ?: "Unbekannt"
                    val status = doc.getString("status") ?: "Fahrbereit"
                    val picUrl = doc.getString("profileImageUrl") ?: ""

                    if (geo != null) {
                        combinedList.add(TruckerBuddy(doc.id, geo.latitude, geo.longitude, name, status, picUrl))
                    }
                }
            }
            otherTrucks = combinedList
        }
    }

    // 2. SENDEN (GPS Update an Server)
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (userId != null) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    firestore.collection("users").document(userId)
                        .update("currentLocation", geoPoint)
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 50f, locationListener)
        } catch (e: SecurityException) {}
        onDispose { locationManager.removeUpdates(locationListener) }
    }

    // 3. UI
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                Configuration.getInstance().userAgentValue = ctx.packageName

                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(OsmGeoPoint(51.1657, 10.4515)) // Start in DE (Kassel)
                    mapController = controller

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    this.overlays.add(locationOverlay)
                    myLocationOverlay = locationOverlay

                    if (isNightMode) {
                        // Dein genialer Dark-Mode Filter
                        val matrix = android.graphics.ColorMatrix()
                        matrix.setSaturation(0f) // S/W
                        // Invertieren + Thub-Tint (vereinfacht für Kürze, nimm deinen Original-Block wenn du magst)
                        val inverseMatrix = android.graphics.ColorMatrix(
                            floatArrayOf(
                                -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                                0.0f, -1.0f, 0.0f, 0.0f, 255f,
                                0.0f, 0.0f, -1.0f, 0.0f, 255f,
                                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                            )
                        )
                        val thubTint = android.graphics.ColorMatrix(
                            floatArrayOf(0.7f, 0f, 0f, 0f, 0f, 0f, 1.0f, 0f, 0f, 0f, 0f, 0f, 1.3f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
                        )
                        matrix.postConcat(inverseMatrix)
                        matrix.postConcat(thubTint)
                        this.overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(matrix))
                    }
                }
            },
            update = { mapView ->
                mapView.overlays.removeIf { it is Marker && it.id != "MY_LOC" }

                // Mein aktueller Standort für Radius-Berechnung
                val myLoc = myLocationOverlay?.myLocation

                otherTrucks.forEach { buddy ->
                    // RADIUS CHECK (70 km = 70000 Meter)
                    val distance = if (myLoc != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(myLoc.latitude, myLoc.longitude, buddy.lat, buddy.lon, results)
                        results[0]
                    } else {
                        0f // Wenn wir kein GPS haben, zeigen wir erstmal alles (oder Dummies)
                    }

                    // Nur anzeigen wenn < 70km (oder Test-Dummy)
                    if (distance < 70000 || buddy.uid.startsWith("dummy")) {
                        val marker = Marker(mapView)
                        marker.position = OsmGeoPoint(buddy.lat, buddy.lon)
                        marker.title = buddy.name
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                        // KLICK LISTENER
                        marker.setOnMarkerClickListener { _, _ ->
                            selectedBuddy = buddy // Öffnet den Dialog
                            true
                        }

                        val ringColor = when(buddy.status) {
                            "Fahrbereit" -> Color.Green
                            "Laden/Entl." -> Color(0xFFFFA500)
                            "Pause" -> ThubNeonBlue
                            else -> Color.Gray
                        }

                        coroutineScope.launch {
                            val customIcon = getAvatarMarker(context, buddy.profileImageUrl, ringColor)
                            marker.icon = customIcon
                            mapView.invalidate()
                        }
                        mapView.overlays.add(marker)
                    }
                }
            }
        )

        // Zentrieren Button
        FloatingActionButton(
            onClick = {
                myLocationOverlay?.enableFollowLocation()
                val loc = myLocationOverlay?.myLocation
                if (loc != null) mapController?.animateTo(loc)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = ThubDarkGray, contentColor = ThubNeonBlue
        ) { Icon(Icons.Filled.MyLocation, "Zentrieren") }

        // --- MINI PROFIL DIALOG (Wenn man auf Truck klickt) ---
        if (selectedBuddy != null) {
            val buddy = selectedBuddy!!
            Dialog(onDismissRequest = { selectedBuddy = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Großes Profilbild
                        AsyncImage(
                            model = buddy.profileImageUrl.ifEmpty { R.drawable.thub_logo_bg },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, ThubNeonBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(buddy.name, style = MaterialTheme.typography.headlineSmall, color = ThubNeonBlue, fontWeight = FontWeight.Bold)
                        Text("Status: ${buddy.status}", color = TextWhite)
                        Text("Fährt: ${buddy.truckType}", color = Color.Gray)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons
                        Button(
                            onClick = { /* TODO: Freundschaftsanfrage */ },
                            colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AddReaction, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Freund hinzufügen", color = Color.Black)
                        }

                        TextButton(onClick = { selectedBuddy = null }) {
                            Text("Schließen", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER (Avatar Marker malen) ---
private suspend fun getAvatarMarker(context: Context, url: String, ringColor: Color): Drawable {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url.ifEmpty { R.drawable.thub_logo_bg })
            .allowHardware(false)
            .build()

        val result = loader.execute(request)
        val rawBitmap = (result.drawable as? BitmapDrawable)?.bitmap
            ?: BitmapFactory.decodeResource(context.resources, R.drawable.thub_logo_bg)

        val size = 120
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = -0xbdbdbe
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        // Bild mittig einpassen
        val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, size, size, false)
        canvas.drawBitmap(scaledBitmap, rect, rect, paint)

        paint.xfermode = null
        paint.style = Paint.Style.STROKE
        paint.color = ringColor.toArgb()
        paint.strokeWidth = 10f // Dicker Ring für bessere Sichtbarkeit
        canvas.drawOval(rectF, paint)

        BitmapDrawable(context.resources, output)
    }
}