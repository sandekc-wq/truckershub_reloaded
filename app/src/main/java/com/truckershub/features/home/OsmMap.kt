package com.truckershub.features.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.location.Location as AndroidLocation
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map // Für Google Maps Icon
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.core.content.ContextCompat
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
import com.truckershub.core.data.model.ParkingSpot
import com.truckershub.core.data.model.ParkingType
import com.truckershub.core.data.model.ParkingRatings
import com.truckershub.core.data.model.AmpelStatus
import com.truckershub.features.parking.ParkingDetailScreen
import com.truckershub.features.parking.ParkingViewModel
import com.truckershub.features.parking.components.ParkingMarkerHelper
import com.truckershub.features.navigation.RouteViewModel
import com.truckershub.features.navigation.components.RoutePlanningPanel
import com.truckershub.features.navigation.components.BorderAlert
import com.truckershub.features.map.SaveLocationDialog
import com.truckershub.features.map.LocationViewModel
import com.truckershub.core.data.model.Location
import com.truckershub.features.map.AddLocationDialog
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
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
    onOpenEUGuide: () -> Unit = {},
    jumpToLocation: Location? = null,
    onLocationJumped: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userId = auth.currentUser?.uid

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isNightMode = currentHour >= 17 || currentHour < 7

    val parkingViewModel: ParkingViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    val savedLocations by locationViewModel.savedLocations.collectAsState()

    val parkingSpots = parkingViewModel.parkingSpots
    var showParkingDetail by remember { mutableStateOf(false) }
    var hasLoadedInitialParkingSpots by remember { mutableStateOf(false) }

    // DIALOG STEUERUNG
    var showAddParkingDialog by remember { mutableStateOf(false) }
    var showSaveLocationDialog by remember { mutableStateOf(false) }

    var clickedPoint by remember { mutableStateOf<OsmGeoPoint?>(null) }
    var myMapView by remember { mutableStateOf<MapView?>(null) }

    // ROUTING STATE
    val currentRoute = routeViewModel.currentRoute
    var isRoutePlanningVisible by remember { mutableStateOf(true) }
    var isRoutePlanningExpanded by remember { mutableStateOf(true) }

    // NEU: Live-Entfernung zum Ziel
    var liveDistanceToDest by remember { mutableStateOf<Int?>(null) }

    val borderLocation = routeViewModel.borderLocation
    val nextBorderCountry = routeViewModel.nextBorderCountry
    var showBorderAlert by remember { mutableStateOf(false) }
    var distanceToBorder by remember { mutableIntStateOf(0) }

    var showSosDialog by remember { mutableStateOf(false) }

    var otherTrucks by remember { mutableStateOf<List<TruckerBuddy>>(emptyList()) }
    var selectedBuddy by remember { mutableStateOf<TruckerBuddy?>(null) }
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    var myLocationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }

    // --- PERMISSIONS ---
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            Toast.makeText(context, "GPS aktiviert! 🛰️", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // --- DATEN LADEN ---
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
            if (hasLocationPermission) {
                myLocationOverlay?.myLocation?.let { myLoc ->
                    parkingViewModel.loadParkingSpotsNearby(
                        GeoPoint(myLoc.latitude, myLoc.longitude),
                        radiusKm = 50.0
                    )
                }
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

    // --- LOCATION TRACKING ---
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: AndroidLocation) {
                    if (userId != null) {
                        firestore.collection("users").document(userId).update("currentLocation", GeoPoint(location.latitude, location.longitude))
                    }
                    if (borderLocation != null && nextBorderCountry != null) {
                        val myLoc = AndroidLocation("me").apply { latitude = location.latitude; longitude = location.longitude }
                        val borderLoc = AndroidLocation("border").apply { latitude = borderLocation.latitude; longitude = borderLocation.longitude }
                        val distMeters = myLoc.distanceTo(borderLoc)
                        distanceToBorder = (distMeters / 1000).toInt()
                        showBorderAlert = distMeters < 10000
                    }
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 10f, locationListener)
            } catch (e: SecurityException) { }
            onDispose { locationManager.removeUpdates(locationListener) }
        } else {
            onDispose { }
        }
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
                    if (hasLocationPermission) {
                        locationOverlay.enableMyLocation()
                        locationOverlay.enableFollowLocation()
                    }
                    this.overlays.add(locationOverlay)
                    myLocationOverlay = locationOverlay

                    val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: OsmGeoPoint?): Boolean { return false }
                        override fun longPressHelper(p: OsmGeoPoint?): Boolean {
                            if (p != null) {
                                clickedPoint = p
                                showSaveLocationDialog = true
                            }
                            return true
                        }
                    })
                    this.overlays.add(mapEventsOverlay)

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

                // --- AUTOPILOT ---
                if (jumpToLocation != null) {
                    myLocationOverlay?.disableFollowLocation() // Fessel lösen
                    val target = OsmGeoPoint(jumpToLocation.latitude, jumpToLocation.longitude)
                    mapView.controller.animateTo(target)
                    mapView.controller.setZoom(18.5)

                    // REPARATUR 1: Nur Name benutzen, da wir kein Adressfeld haben
                    val destText = jumpToLocation.name
                    routeViewModel.updateDestinationPoint(destText)

                    if (routeViewModel.startText.isEmpty()) routeViewModel.updateStartPoint("Mein Standort")
                    isRoutePlanningExpanded = true
                    onLocationJumped()
                }

                if (hasLocationPermission && myLocationOverlay?.isMyLocationEnabled == false) {
                    myLocationOverlay?.enableMyLocation()
                }

                // --- LIVE ENTFERNUNG BERECHNEN ---
                if (currentRoute != null && hasLocationPermission && myLocationOverlay?.myLocation != null) {
                    val myLoc = myLocationOverlay!!.myLocation // Das ist ein OsmGeoPoint
                    val routePoints = decodePolyline(currentRoute.routeDetails.points)

                    if (routePoints.isNotEmpty()) {
                        val destPoint = routePoints.last()

                        // REPARATUR 2: distanceToAsDouble verwenden (Osmdroid zu Osmdroid)
                        val distMeters = myLoc.distanceToAsDouble(destPoint)
                        liveDistanceToDest = distMeters.toInt()
                    }
                }

                // Marker aufräumen
                mapView.overlays.removeIf {
                    it is Marker && it.id != "MY_LOC" && it.id != "ROUTE_START" && it.id != "ROUTE_END" && it.id != "TEMP_MARKER"
                }

                if (currentRoute == null) {
                    mapView.overlays.removeIf { it is Polyline }
                    liveDistanceToDest = null // Reset
                } else {
                    val hasPolyline = mapView.overlays.any { it is Polyline }
                    if (!hasPolyline) {
                        val encodedString = currentRoute.routeDetails.points
                        val points = decodePolyline(encodedString)
                        val polyline = Polyline()
                        polyline.setPoints(points)
                        polyline.outlinePaint.color = ThubNeonBlue.toArgb()
                        polyline.outlinePaint.strokeWidth = 20f
                        polyline.outlinePaint.strokeCap = Paint.Cap.ROUND
                        mapView.overlays.add(0, polyline)
                    }
                }

                // Marker Updates
                ParkingMarkerHelper.updateParkingMarkers(mapView, parkingSpots) { p -> parkingViewModel.selectParking(p.id); showParkingDetail = true }
                savedLocations.forEach { loc ->
                    val marker = Marker(mapView)
                    marker.id = "SAVED_LOC_${loc.id}"
                    marker.position = OsmGeoPoint(loc.latitude, loc.longitude)
                    marker.title = "${loc.name} (${loc.type})"
                    marker.snippet = loc.description
                    val star = ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on)
                    star?.setTint(ThubNeonBlue.toArgb())
                    marker.icon = star
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                    mapView.overlays.add(marker)
                }
                otherTrucks.forEach { buddy ->
                    val marker = Marker(mapView)
                    marker.position = OsmGeoPoint(buddy.lat, buddy.lon)
                    marker.title = buddy.name
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marker.setOnMarkerClickListener { _, _ -> selectedBuddy = buddy; true }
                    val rColor = when(buddy.status) { "Fahrbereit" -> Color.Green; "Pause" -> ThubNeonBlue; else -> Color.Gray }
                    coroutineScope.launch {
                        val ico = getAvatarMarker(context, buddy.profileImageUrl, rColor)
                        marker.icon = ico
                        mapView.invalidate()
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        // Fadenkreuz
        if (currentRoute == null) {
            Icon(Icons.Filled.Add, "Ziel", tint = Color.Red.copy(0.8f), modifier = Modifier.align(Alignment.Center).size(48.dp))
        }

        // --- PANEL: ROUTEN PLANUNG ---
        if (isRoutePlanningVisible && currentRoute == null) {
            RoutePlanningPanel(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                expanded = isRoutePlanningExpanded,
                onExpandToggle = { isRoutePlanningExpanded = !isRoutePlanningExpanded },
                startText = routeViewModel.startText,
                destinationText = routeViewModel.destinationText,
                onStartChanged = { routeViewModel.updateStartPoint(it) },
                onDestinationChanged = { routeViewModel.updateDestinationPoint(it) },
                onWaypointAdded = {},
                onCalculateRoute = {
                    val currentLoc = myLocationOverlay?.myLocation?.let { GeoPoint(it.latitude, it.longitude) }
                    routeViewModel.calculateRoute(context, currentLoc) {
                        isRoutePlanningExpanded = false
                        myLocationOverlay?.enableFollowLocation()
                        mapController?.setZoom(19.0)
                        mapController?.animateTo(myLocationOverlay?.myLocation)
                    }
                },
                onMinimize = { isRoutePlanningExpanded = false },
                onSosClick = { showSosDialog = true }
            )
        }

        // --- PANEL: LIVE INFO WÄHREND DER FAHRT ---
        if (currentRoute != null) {
            val rawDistance = liveDistanceToDest ?: currentRoute.routeDetails.instructions.firstOrNull()?.distance?.toInt() ?: 0
            val formattedDistance = formatDistance(rawDistance)

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThubBlack.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Directions, null, tint = ThubNeonBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Entfernung zum Ziel:", color = Color.Gray, fontSize = 12.sp)
                            Text(formattedDistance, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }

                        // --- NAVI BUTTON ---
                        Button(
                            onClick = {
                                val destPoints = decodePolyline(currentRoute.routeDetails.points)
                                if (destPoints.isNotEmpty()) {
                                    val dest = destPoints.last()
                                    val uri = Uri.parse("google.navigation:q=${dest.latitude},${dest.longitude}&mode=d") // d = driving
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Google Maps nicht gefunden", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = ThubBlack)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NAVI", color = ThubBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (routeViewModel.isCalculating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = ThubNeonBlue)
        if (routeViewModel.errorMessage != null) Text(routeViewModel.errorMessage!!, color = Color.Red, modifier = Modifier.align(Alignment.Center).background(Color.Black).padding(8.dp))

        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
            BorderAlert(nextBorderCountry, showBorderAlert, distanceToBorder) { showBorderAlert = false; onOpenEUGuide() }
        }

        // BUTTONS UNTEN
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (currentRoute != null) {
                FloatingActionButton(onClick = { routeViewModel.resetRoute(); myMapView?.invalidate(); liveDistanceToDest = null; Toast.makeText(context, "Route beendet", Toast.LENGTH_SHORT).show() }, containerColor = Color.Red, contentColor = Color.White) { Icon(Icons.Filled.Close, "Abbruch") }
                Spacer(modifier = Modifier.height(8.dp))
            }
            FloatingActionButton(onClick = { val center = myMapView?.mapCenter; if (center != null && center.latitude != 0.0) { clickedPoint = OsmGeoPoint(center.latitude, center.longitude); showAddParkingDialog = true } }, containerColor = if(currentRoute != null) ThubDarkGray else Color.Red, contentColor = if(currentRoute != null) Color.Gray else Color.White) { Icon(Icons.Filled.AddLocationAlt, "Ort melden") }
            FloatingActionButton(onClick = { if (hasLocationPermission) { myLocationOverlay?.enableFollowLocation(); val loc = myLocationOverlay?.myLocation; if (loc != null) { mapController?.animateTo(loc); mapController?.setZoom(if(currentRoute != null) 19.0 else 15.0); if(currentRoute == null) parkingViewModel.loadParkingSpotsNearby(GeoPoint(loc.latitude, loc.longitude), 50.0) } } else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) }, containerColor = ThubDarkGray, contentColor = ThubNeonBlue) { Icon(Icons.Filled.MyLocation, "Zentrieren") }
        }

        if (showSosDialog) { val myLoc = myLocationOverlay?.myLocation; EmergencyDialog(myLoc?.latitude ?: 0.0, myLoc?.longitude ?: 0.0) { showSosDialog = false } }

        if (selectedBuddy != null) {
            val buddy = selectedBuddy!!
            Dialog(onDismissRequest = { selectedBuddy = null }) {
                Card(colors = CardDefaults.cardColors(containerColor = ThubBlack), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().border(2.dp, ThubNeonBlue, RoundedCornerShape(24.dp))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = { selectedBuddy = null }) { Icon(Icons.Filled.Close, null, tint = ThubNeonBlue) } }
                        AsyncImage(model = buddy.profileImageUrl.ifEmpty { R.drawable.thub_logo_bg }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp).clip(CircleShape))
                        Text(buddy.name, style = MaterialTheme.typography.headlineMedium, color = TextWhite)
                        ThubDialogButton("Profil ansehen", Icons.Filled.Person, { selectedBuddy = null; onOpenProfile(buddy.uid) }, true)
                    }
                }
            }
        }

        if (showSaveLocationDialog && clickedPoint != null) { SaveLocationDialog(clickedPoint!!.latitude, clickedPoint!!.longitude, { showSaveLocationDialog = false }) { locationViewModel.saveLocation(it); showSaveLocationDialog = false } }

        // REPARATUR 3: Named Arguments für ParkingSpot, damit nichts verrutscht!
        if (showAddParkingDialog && clickedPoint != null) {
            AddLocationDialog(clickedPoint!!.latitude, clickedPoint!!.longitude, { showAddParkingDialog = false }) { locData ->
                val pType = when(locData.type) { "Parkplatz" -> ParkingType.PARKPLATZ; "Raststätte" -> ParkingType.RASTSTAETTE; "Autohof" -> ParkingType.AUTOHOF; else -> ParkingType.UNKNOWN }
                val capInt = locData.capacity.toIntOrNull() ?: 0
                val facilitiesList = mutableListOf<String>(); if (locData.hasShower) facilitiesList.add("Dusche"); if (locData.hasFood) facilitiesList.add("Essen"); if (locData.hasWifi) facilitiesList.add("WLAN")

                val firestoreGeo = GeoPoint(clickedPoint!!.latitude, clickedPoint!!.longitude)

                val newParkingSpotData = hashMapOf(
                    "name" to locData.name, "type" to pType.name, "description" to locData.description,
                    "truckCapacity" to capInt, "isPaid" to locData.isPaid, "facilities" to facilitiesList,
                    "address" to locData.address, "location" to firestoreGeo,
                    "reportedBy" to (auth.currentUser?.uid ?: "anonymous"),
                    "lastAmpelUpdate" to System.currentTimeMillis(),
                    "currentAmpel" to "UNKNOWN", "ratings" to hashMapOf("overall" to 0.0, "totalReviews" to 0)
                )

                firestore.collection("parkingSpots").add(newParkingSpotData).addOnSuccessListener { doc ->
                    showAddParkingDialog = false
                    val newSpot = ParkingSpot(
                        id = doc.id,
                        name = locData.name,
                        type = pType,
                        description = locData.description,
                        truckCapacity = capInt,
                        isPaid = locData.isPaid,
                        facilities = facilitiesList,
                        address = locData.address,
                        location = firestoreGeo,
                        reportedBy = (auth.currentUser?.uid ?: "anonymous"),
                        currentAmpel = AmpelStatus.UNKNOWN,
                        ratings = ParkingRatings()
                    )
                    parkingViewModel.addTemporarySpot(newSpot)
                    Toast.makeText(context, "Gespeichert!", Toast.LENGTH_SHORT).show()
                }
            } }

        if (showParkingDetail && parkingViewModel.selectedParking != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).padding(16.dp)) {
                ParkingDetailScreen(parkingViewModel.selectedParking!!, parkingViewModel.reviews, { showParkingDetail = false; parkingViewModel.clearSelection() }, { s, c -> parkingViewModel.selectedParking?.let { parkingViewModel.reportAmpelStatus(it.id, s, c) } }, { parkingViewModel.submitReview(it) }, { parkingViewModel.selectedParking?.let { val i = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${it.location.latitude},${it.location.longitude}&mode=d")); i.setPackage("com.google.android.apps.maps"); context.startActivity(i) } })
            }
        }
    }
}

// --- HELFER: METER IN KM UMRECHNEN ---
fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        val km = meters / 1000.0
        String.format("%.1f km", km)
    } else {
        "$meters m"
    }
}

// Helper Polyline etc
private fun decodePolyline(encoded: String): List<OsmGeoPoint> { val poly = ArrayList<OsmGeoPoint>(); var index = 0; val len = encoded.length; var lat = 0; var lng = 0; while (index < len) { var b: Int; var shift = 0; var result = 0; do { b = encoded[index++].code - 63; result = result or (b and 0x1f shl shift); shift += 5 } while (b >= 0x20); val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1; lat += dlat; shift = 0; result = 0; do { b = encoded[index++].code - 63; result = result or (b and 0x1f shl shift); shift += 5 } while (b >= 0x20); val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1; lng += dlng; val p = OsmGeoPoint(lat / 1E5, lng / 1E5); poly.add(p) }; return poly }
private suspend fun getAvatarMarker(context: Context, url: String, ringColor: Color): Drawable { return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { val loader = ImageLoader(context); val request = ImageRequest.Builder(context).data(url.ifEmpty { R.drawable.thub_logo_bg }).allowHardware(false).build(); val result = loader.execute(request); val rawBitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: BitmapFactory.decodeResource(context.resources, R.drawable.thub_logo_bg); val size = 120; val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); val canvas = Canvas(output); val paint = Paint(); val rect = Rect(0, 0, size, size); val rectF = RectF(rect); paint.isAntiAlias = true; canvas.drawARGB(0, 0, 0, 0); paint.color = -0xbdbdbe; canvas.drawOval(rectF, paint); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN); val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, size, size, false); canvas.drawBitmap(scaledBitmap, rect, rect, paint); paint.xfermode = null; paint.style = Paint.Style.STROKE; paint.color = ringColor.toArgb(); paint.strokeWidth = 10f; canvas.drawOval(rectF, paint); BitmapDrawable(context.resources, output) } }
@Composable private fun ThubDialogButton(text: String, icon: ImageVector, onClick: () -> Unit, isSecondary: Boolean = false) { val interactionSource = remember { MutableInteractionSource() }; val isPressed by interactionSource.collectIsPressedAsState(); val scale by animateFloatAsState(if (isPressed) 0.96f else 1f); val baseColor = if (isSecondary) ThubDarkGray else ThubNeonBlue; val textColor = if (isSecondary) ThubNeonBlue else ThubBlack; Box(modifier = Modifier.fillMaxWidth().height(50.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null, onClick = onClick).clip(RoundedCornerShape(12.dp)).background(baseColor).border(if (isSecondary) 1.dp else 0.dp, ThubNeonBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = textColor); Spacer(modifier = Modifier.width(12.dp)); Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp) } } }