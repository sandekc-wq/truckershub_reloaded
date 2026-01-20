package com.truckershub.features.home

import android.content.Context
import android.content.Intent
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
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.truckershub.features.parking.ParkingDetailScreen
import com.truckershub.features.parking.ParkingViewModel
import com.truckershub.features.parking.components.ParkingMarkerHelper
import com.truckershub.features.navigation.RouteViewModel
import com.truckershub.features.navigation.components.RoutePlanningPanel
// IMPORT FÜR DEN NEUEN DIALOG
import com.truckershub.features.map.AddLocationDialog
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Calendar

// --- DATENMODELL ---
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

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isNightMode = currentHour >= 17 || currentHour < 7

    // ViewModels
    val parkingViewModel: ParkingViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()

    val parkingSpots = parkingViewModel.parkingSpots
    var showParkingDetail by remember { mutableStateOf(false) }
    var hasLoadedInitialParkingSpots by remember { mutableStateOf(false) }

    // NEUE STATES FÜR DAS MELDEN
    var showAddDialog by remember { mutableStateOf(false) }
    var clickedPoint by remember { mutableStateOf<OsmGeoPoint?>(null) }
    // Wir brauchen Zugriff auf die MapView Instanz
    var myMapView by remember { mutableStateOf<MapView?>(null) }

    // Route-Planung UI States
    var isRoutePlanningVisible by remember { mutableStateOf(true) }
    var isRoutePlanningExpanded by remember { mutableStateOf(false) }

    var otherTrucks by remember { mutableStateOf<List<TruckerBuddy>>(emptyList()) }
    var selectedBuddy by remember { mutableStateOf<TruckerBuddy?>(null) }
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    var myLocationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }

    // Dummies
    val dummies = remember {
        listOf(
            TruckerBuddy("dummy1", 53.402, 8.448, "Diesel-Dieter", "Pause", "", "Sattelzug"),
            TruckerBuddy("dummy2", 53.330, 8.480, "Logistik-Lisa", "Fahrbereit", "", "Tankwagen")
        )
    }

    LaunchedEffect(Unit) {
        if (!hasLoadedInitialParkingSpots) {
            val defaultLocation = GeoPoint(51.1657, 10.4515)
            parkingViewModel.loadParkingSpotsNearby(defaultLocation, radiusKm = 100.0)
            hasLoadedInitialParkingSpots = true
        }

        firestore.collection("users").addSnapshotListener { snapshot, _ ->
            val combinedList = dummies.toMutableList()
            myLocationOverlay?.myLocation?.let { myLoc ->
                parkingViewModel.loadParkingSpotsNearby(
                    GeoPoint(myLoc.latitude, myLoc.longitude),
                    radiusKm = 50.0
                )
            }
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

    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (userId != null) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    firestore.collection("users").document(userId).update("currentLocation", geoPoint)
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
                    controller.setCenter(OsmGeoPoint(51.1657, 10.4515))
                    mapController = controller

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    this.overlays.add(locationOverlay)
                    myLocationOverlay = locationOverlay

                    // DARK MODE MAGIC ✨
                    if (isNightMode) {
                        val matrix = android.graphics.ColorMatrix()
                        matrix.setSaturation(0f)
                        val inverseMatrix = android.graphics.ColorMatrix(floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f))
                        val thubTint = android.graphics.ColorMatrix(floatArrayOf(0.7f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1.3f,0f,0f, 0f,0f,0f,1f,0f))
                        matrix.postConcat(inverseMatrix)
                        matrix.postConcat(thubTint)
                        this.overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(matrix))
                    }
                }
            },
            update = { mapView ->
                myMapView = mapView // Instanz merken!

                mapView.overlays.removeIf { it is Marker && it.id != "MY_LOC" && it.id != "ROUTE_START" && it.id != "ROUTE_END" }
                mapView.overlays.removeIf { it is Polyline }

                // --- ROUTE ZEICHNEN ---
                val route = routeViewModel.currentRoute
                if (route != null) {
                    val encodedString = route.routeDetails.points
                    val points = decodePolyline(encodedString)

                    val polyline = Polyline()
                    polyline.setPoints(points)
                    polyline.outlinePaint.color = ThubNeonBlue.toArgb()
                    polyline.outlinePaint.strokeWidth = 15f
                    polyline.outlinePaint.strokeCap = Paint.Cap.ROUND

                    mapView.overlays.add(0, polyline)
                }

                ParkingMarkerHelper.updateParkingMarkers(
                    mapView = mapView,
                    parkings = parkingSpots,
                    onParkingClick = { parking ->
                        parkingViewModel.selectParking(parking.id)
                        showParkingDetail = true
                    }
                )

                otherTrucks.forEach { buddy ->
                    val marker = Marker(mapView)
                    marker.position = OsmGeoPoint(buddy.lat, buddy.lon)
                    marker.title = buddy.name
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marker.setOnMarkerClickListener { _, _ ->
                        selectedBuddy = buddy
                        true
                    }
                    val rColor = when(buddy.status) {
                        "Fahrbereit" -> Color.Green
                        "Laden/Entl." -> Color(0xFFFFA500)
                        "Pause" -> ThubNeonBlue
                        else -> Color.Gray
                    }
                    coroutineScope.launch {
                        val iconDrawable = getAvatarMarker(context, buddy.profileImageUrl, rColor)
                        marker.icon = iconDrawable
                        mapView.invalidate()
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        // DAS ROTE FADENKREUZ (Mitte) 🎯
        Icon(
            Icons.Filled.Add,
            contentDescription = "Zielmitte",
            tint = Color.Red.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.Center).size(48.dp)
        )

        if (isRoutePlanningVisible) {
            RoutePlanningPanel(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                expanded = isRoutePlanningExpanded,
                onExpandToggle = { isRoutePlanningExpanded = !isRoutePlanningExpanded },
                onStartChanged = { routeViewModel.updateStartPoint(it) },
                onDestinationChanged = { routeViewModel.updateDestinationPoint(it) },
                onWaypointAdded = {},
                onCalculateRoute = {
                    routeViewModel.calculateRoute()
                },
                onMinimize = { isRoutePlanningExpanded = false },
                currentLocation = myLocationOverlay?.myLocation?.let {
                    GeoPoint(it.latitude, it.longitude)
                }
            )

            if (routeViewModel.isCalculating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = ThubNeonBlue
                )
            }
            if (routeViewModel.errorMessage != null) {
                Text(
                    text = routeViewModel.errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center).background(Color.Black).padding(8.dp)
                )
            }
        }

        // BUTTON-STACK UNTEN RECHTS (Melden + Zentrieren)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. NEUER BUTTON: ORT MELDEN (Rot) 🔴
            FloatingActionButton(
                onClick = {
                    val center = myMapView?.mapCenter
                    if (center != null && center.latitude != 0.0) {
                        clickedPoint = OsmGeoPoint(center.latitude, center.longitude)
                        showAddDialog = true
                    } else {
                        Toast.makeText(context, "Karte wird noch geladen...", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.AddLocationAlt, "Ort melden")
            }

            // 2. ALTER BUTTON: ZENTRIEREN (Blau) 🔵
            FloatingActionButton(
                onClick = {
                    myLocationOverlay?.enableFollowLocation()
                    val loc = myLocationOverlay?.myLocation
                    if (loc != null) {
                        mapController?.animateTo(loc)
                        // Wenn zentriert wird, laden wir auch gleich neue Parkplätze
                        val searchPoint = GeoPoint(loc.latitude, loc.longitude)
                        parkingViewModel.loadParkingSpotsNearby(searchPoint, 50.0)
                    }
                },
                containerColor = ThubDarkGray,
                contentColor = ThubNeonBlue
            ) {
                Icon(Icons.Filled.MyLocation, "Zentrieren")
            }
        }

        // DIALOGS & OVERLAYS

        if (selectedBuddy != null) {
            val buddy = selectedBuddy!!
            Dialog(onDismissRequest = { selectedBuddy = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = buddy.profileImageUrl.ifEmpty { R.drawable.thub_logo_bg }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, ThubNeonBlue, CircleShape))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(buddy.name, style = MaterialTheme.typography.headlineSmall, color = ThubNeonBlue, fontWeight = FontWeight.Bold)
                        Text("Status: ${buddy.status}", color = TextWhite)
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(onClick = { selectedBuddy = null }) { Text("Schließen", color = Color.Gray) }
                    }
                }
            }
        }

        // DER NEUE DIALOG ZUM MELDEN
        if (showAddDialog && clickedPoint != null) {
            AddLocationDialog(
                lat = clickedPoint!!.latitude,
                lng = clickedPoint!!.longitude,
                onDismiss = { showAddDialog = false },
                onSave = { name, type, desc ->
                    println("TruckersHub: Neuer Ort '$name' ($type)")
                    // Hier später an Firestore anbinden!
                    showAddDialog = false
                    Toast.makeText(context, "Danke! '$name' gemeldet. ✅", Toast.LENGTH_LONG).show()
                }
            )
        }

        if (showParkingDetail && parkingViewModel.selectedParking != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).padding(16.dp)) {
                ParkingDetailScreen(
                    parking = parkingViewModel.selectedParking!!,
                    reviews = parkingViewModel.reviews,
                    onBack = {
                        showParkingDetail = false
                        parkingViewModel.clearSelection()
                    },
                    onReportStatus = { status, comment ->
                        parkingViewModel.selectedParking?.let { parking ->
                            parkingViewModel.reportAmpelStatus(parking.id, status, comment)
                        }
                    },
                    onSubmitReview = { review ->
                        parkingViewModel.submitReview(review)
                    },
                    onNavigate = {
                        parkingViewModel.selectedParking?.let { parking ->
                            val lat = parking.location.latitude
                            val lon = parking.location.longitude
                            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try { context.startActivity(mapIntent) } catch (e: Exception) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon"))
                                context.startActivity(browserIntent)
                            }
                        }
                    }
                )
            }
        }
    }
}

// === HILFSFUNKTIONEN ===

private fun decodePolyline(encoded: String): List<OsmGeoPoint> {
    val poly = ArrayList<OsmGeoPoint>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = OsmGeoPoint(lat / 1E5, lng / 1E5)
        poly.add(p)
    }
    return poly
}

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
        val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, size, size, false)
        canvas.drawBitmap(scaledBitmap, rect, rect, paint)
        paint.xfermode = null
        paint.style = Paint.Style.STROKE
        paint.color = ringColor.toArgb()
        paint.strokeWidth = 10f
        canvas.drawOval(rectF, paint)

        BitmapDrawable(context.resources, output)
    }
}