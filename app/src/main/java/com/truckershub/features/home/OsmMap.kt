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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning // <--- Für den SOS Button
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.truckershub.R
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.features.parking.ParkingDetailScreen
import com.truckershub.features.parking.ParkingViewModel
import com.truckershub.features.parking.components.ParkingMarkerHelper
import com.truckershub.features.navigation.RouteViewModel
import com.truckershub.features.navigation.components.RoutePlanningPanel
import com.truckershub.features.navigation.components.BorderAlert
import com.truckershub.features.map.AddLocationDialog
import com.truckershub.features.map.LocationData
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
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    onOpenProfile: (String) -> Unit,
    onOpenEUGuide: () -> Unit = {}
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

    // STATES FÜR DAS MELDEN
    var showAddDialog by remember { mutableStateOf(false) }
    var clickedPoint by remember { mutableStateOf<OsmGeoPoint?>(null) }
    var myMapView by remember { mutableStateOf<MapView?>(null) }

    // Route-Planung UI States
    var isRoutePlanningVisible by remember { mutableStateOf(true) }
    var isRoutePlanningExpanded by remember { mutableStateOf(false) }

    // --- AUTOMATIK: GRENZ-ÜBERWACHUNG 📡 ---
    val borderLocation = routeViewModel.borderLocation
    val nextBorderCountry = routeViewModel.nextBorderCountry

    // UI State für den Alarm
    var showBorderAlert by remember { mutableStateOf(false) }
    var distanceToBorder by remember { mutableIntStateOf(0) }

    // --- SOS BUTTON STATE 🚨 ---
    var showSosDialog by remember { mutableStateOf(false) }

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

                // === AUTOMATISCHER GRENZ-CHECK 🛰️ ===
                if (borderLocation != null && nextBorderCountry != null) {
                    val myLoc = Location("me")
                    myLoc.latitude = location.latitude
                    myLoc.longitude = location.longitude

                    val borderLoc = Location("border")
                    borderLoc.latitude = borderLocation.latitude
                    borderLoc.longitude = borderLocation.longitude

                    val distMeters = myLoc.distanceTo(borderLoc)
                    distanceToBorder = (distMeters / 1000).toInt()

                    if (distMeters < 10000) {
                        showBorderAlert = true
                    } else {
                        if (distMeters > 15000) showBorderAlert = false
                    }
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 10f, locationListener)
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

                    // DARK MODE
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
                myMapView = mapView

                mapView.overlays.removeIf {
                    it is Marker &&
                            it.id != "MY_LOC" &&
                            it.id != "ROUTE_START" &&
                            it.id != "ROUTE_END" &&
                            it.id != "TEMP_MARKER"
                }
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

        // FADENKREUZ
        Icon(
            Icons.Filled.Add,
            contentDescription = "Zielmitte",
            tint = Color.Red.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.Center).size(48.dp)
        )

        // Route Panel
        if (isRoutePlanningVisible) {
            RoutePlanningPanel(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                expanded = isRoutePlanningExpanded,
                onExpandToggle = { isRoutePlanningExpanded = !isRoutePlanningExpanded },
                onStartChanged = { routeViewModel.updateStartPoint(it) },
                onDestinationChanged = { routeViewModel.updateDestinationPoint(it) },
                onWaypointAdded = {},
                onCalculateRoute = { routeViewModel.calculateRoute() },
                onMinimize = { isRoutePlanningExpanded = false },
                // HIER IST DIE NEUE LEITUNG: 👇
                onSosClick = { showSosDialog = true },
                currentLocation = myLocationOverlay?.myLocation?.let { GeoPoint(it.latitude, it.longitude) }
            )
            if (routeViewModel.isCalculating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = ThubNeonBlue)
            }
            if (routeViewModel.errorMessage != null) {
                Text(
                    text = routeViewModel.errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center).background(Color.Black).padding(8.dp)
                )
            }
        }

        // --- BORDER ALERT (Automatisch) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            BorderAlert(
                country = nextBorderCountry,
                isVisible = showBorderAlert,
                distanceKm = distanceToBorder,
                onOpenInfo = {
                    showBorderAlert = false
                    onOpenEUGuide()
                }
            )
        }



        // BUTTONS UNTEN RECHTS
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MELDEN
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

            // ZENTRIEREN
            FloatingActionButton(
                onClick = {
                    myLocationOverlay?.enableFollowLocation()
                    val loc = myLocationOverlay?.myLocation
                    if (loc != null) {
                        mapController?.animateTo(loc)
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

        // --- DIALOGE ---

        // 1. SOS DIALOG
        if (showSosDialog) {
            val myLoc = myLocationOverlay?.myLocation
            val lat = myLoc?.latitude ?: 0.0
            val lon = myLoc?.longitude ?: 0.0

            EmergencyDialog(
                currentLat = lat,
                currentLon = lon,
                onDismiss = { showSosDialog = false }
            )
        }

        // 2. BUDDY POPUP
        if (selectedBuddy != null) {
            val buddy = selectedBuddy!!
            val isMe = buddy.uid == userId

            Dialog(onDismissRequest = { selectedBuddy = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThubBlack),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, ThubNeonBlue, RoundedCornerShape(24.dp))
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = ThubNeonBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { selectedBuddy = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Schließen", tint = ThubNeonBlue)
                            }
                        }

                        val statusColor = when(buddy.status) {
                            "Fahrbereit" -> Color.Green
                            "Pause" -> ThubNeonBlue
                            else -> Color.Gray
                        }
                        AsyncImage(
                            model = buddy.profileImageUrl.ifEmpty { R.drawable.thub_logo_bg },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(4.dp, statusColor, CircleShape)
                                .shadow(8.dp, CircleShape, spotColor = statusColor)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(buddy.name, style = MaterialTheme.typography.headlineMedium, color = TextWhite, fontWeight = FontWeight.Black)
                        Text(buddy.truckType, color = Color.Gray, fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                        ) {
                            Text(
                                text = buddy.status,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (!isMe) {
                            ThubDialogButton(
                                text = "Als Freund hinzufügen",
                                icon = Icons.Filled.PersonAdd,
                                onClick = {
                                    if (userId != null) {
                                        val request = hashMapOf(
                                            "fromId" to userId, "toId" to buddy.uid,
                                            "status" to "pending", "timestamp" to FieldValue.serverTimestamp()
                                        )
                                        firestore.collection("friend_requests").add(request)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Anfrage an ${buddy.name} raus! 📨", Toast.LENGTH_SHORT).show()
                                                selectedBuddy = null
                                            }
                                    } else { Toast.makeText(context, "Bitte einloggen!", Toast.LENGTH_SHORT).show() }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        ThubDialogButton(
                            text = "Profil ansehen",
                            icon = Icons.Filled.Person,
                            isSecondary = true,
                            onClick = {
                                selectedBuddy = null
                                onOpenProfile(buddy.uid)
                            }
                        )
                    }
                }
            }
        }

        // 3. ADD LOCATION DIALOG
        if (showAddDialog && clickedPoint != null) {
            AddLocationDialog(
                lat = clickedPoint!!.latitude,
                lng = clickedPoint!!.longitude,
                onDismiss = { showAddDialog = false },
                onSave = { locationData ->
                    val newParkingSpotData = hashMapOf(
                        "name" to locationData.name,
                        "type" to locationData.type,
                        "description" to locationData.description,
                        "capacity" to locationData.capacity,
                        "isPaid" to locationData.isPaid,
                        "hasShower" to locationData.hasShower,
                        "hasFood" to locationData.hasFood,
                        "hasWifi" to locationData.hasWifi,
                        "address" to locationData.address,
                        "location" to GeoPoint(clickedPoint!!.latitude, clickedPoint!!.longitude),
                        "reportedBy" to (auth.currentUser?.uid ?: "anonymous"),
                        "timestamp" to FieldValue.serverTimestamp(),
                        "status" to "unknown",
                        "rating" to 0.0,
                        "reviewCount" to 0
                    )

                    firestore.collection("parkingSpots")
                        .add(newParkingSpotData)
                        .addOnSuccessListener {
                            showAddDialog = false
                            val newMarker = Marker(myMapView)
                            newMarker.position = clickedPoint
                            newMarker.title = locationData.name
                            newMarker.snippet = "Gerade von DIR gemeldet!"
                            newMarker.id = "TEMP_MARKER"
                            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            myMapView?.overlays?.add(newMarker)
                            myMapView?.invalidate()
                            Toast.makeText(context, "Erfolgreich gespeichert! 🌍✅", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Fehler beim Senden: ${e.message} ❌", Toast.LENGTH_LONG).show()
                        }
                }
            )
        }

        // 4. PARKING DETAIL
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

@Composable
private fun ThubDialogButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isSecondary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    val baseColor = if (isSecondary) ThubDarkGray else ThubNeonBlue
    val textColor = if (isSecondary) ThubNeonBlue else ThubBlack

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                    else listOf(Color.Transparent, Color.Transparent)
                )
            )
            .border(if (isSecondary) 1.dp else 0.dp, ThubNeonBlue, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = textColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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