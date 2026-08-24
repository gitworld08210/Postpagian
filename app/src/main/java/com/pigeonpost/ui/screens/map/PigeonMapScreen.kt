package com.pigeonpost.ui.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.pigeonpost.R
import com.pigeonpost.data.model.MessageStatus
import com.pigeonpost.ui.components.ParchmentBackground
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.DeepBrown800
import com.pigeonpost.ui.theme.GoldAccent400
import com.pigeonpost.ui.theme.GoldAccent500
import com.pigeonpost.ui.theme.Parchment100
import com.pigeonpost.ui.theme.Parchment300
import com.pigeonpost.ui.theme.RoyalBlue800
import com.pigeonpost.ui.theme.WaxSealRed400
import com.pigeonpost.ui.theme.WaxSealRed500
import kotlin.math.abs

/** Number of sampled points used to draw a route leg. */
private const val ROUTE_SAMPLES = 64

/** Smallest span, in degrees, the camera will frame - keeps short hops from over-zooming. */
private const val MIN_BOUNDS_SPAN_DEG = 0.02

/** Padding, in pixels, left around the route when framing the camera. */
private const val BOUNDS_PADDING_PX = 64

/** Zoom used for the very first frame, before the route bounds are applied. */
private const val INITIAL_ZOOM = 5f

/**
 * Shows the pigeon's real position on a real Google Maps chart.
 *
 * Everything plotted here comes from the actual latitude/longitude in
 * [PigeonMapUiState] - the sender's position, the recipient's position, the route, and
 * the bird's live interpolated position at 60 km/h. The map is styled in aged sepia,
 * framed in a decorative border and topped with a compass rose so it still reads as a
 * chart in an old atlas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PigeonMapScreen(
    messageId: String,
    onNavigateBack: () -> Unit,
    viewModel: PigeonMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPermissionGranted by viewModel.locationPermissionGranted
        .collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshLocationPermission()
    }

    ParchmentBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Pigeon Tracker",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Real map, framed like an old chart
                AncientChartFrame(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        uiState.error != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.error ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WaxSealRed500,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }

                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = GoldAccent500)
                            }
                        }

                        else -> {
                            RealPigeonMap(
                                uiState = uiState,
                                showMyLocation = locationPermissionGranted,
                                modifier = Modifier.fillMaxSize()
                            )
                            CompassRose(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (uiState.status) {
                                MessageStatus.FLYING -> "Thy pigeon soars through the skies"
                                MessageStatus.DELIVERED -> "Message delivered by faithful pigeon!"
                                MessageStatus.LOST -> "Alas! Thy pigeon has perished in transit"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when (uiState.status) {
                                MessageStatus.FLYING -> GoldAccent400
                                MessageStatus.DELIVERED -> MaterialTheme.colorScheme.onSurface
                                MessageStatus.LOST -> WaxSealRed500
                            },
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.status == MessageStatus.FLYING) {
                            Text(
                                text = "Progress: ${(uiState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "~${String.format("%.1f", uiState.estimatedHoursRemaining)} hours remaining",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${String.format("%.0f", uiState.distanceKm)} km journey at 60 km/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Decorative double border around the map so the modern tiles still sit inside
 * something that feels like a page torn from an atlas.
 */
@Composable
private fun AncientChartFrame(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(Parchment300)
            .border(width = 3.dp, color = DeepBrown800.copy(alpha = 0.65f))
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = GoldAccent500.copy(alpha = 0.7f))
                .padding(2.dp),
            content = content
        )
    }
}

/**
 * A real Google Map plotting the actual journey, dressed in an aged sepia style.
 *
 * The markers and both polylines are driven straight off [PigeonMapUiState], so the
 * pigeon keeps moving across the chart as the flight ticks along. The camera is fitted
 * to the whole route once the map has finished loading, then the user is left in control.
 *
 * @param showMyLocation only true while a location permission is actually held
 */
@Composable
private fun RealPigeonMap(
    uiState: PigeonMapUiState,
    showMyLocation: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val origin = LatLng(uiState.senderLat, uiState.senderLng)
    val destination = LatLng(uiState.receiverLat, uiState.receiverLng)
    val pigeon = LatLng(uiState.pigeonLat, uiState.pigeonLng)

    // Aged parchment tiles instead of default modern Google colours.
    val mapStyle = remember {
        runCatching {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_aged_atlas)
        }.getOrNull()
    }

    val originIcon = remember {
        MapMarkerIcons.locationPin(
            context = context,
            fillColor = RoyalBlue800.toArgb(),
            ringColor = DeepBrown800.toArgb()
        )
    }
    val destinationIcon = remember {
        MapMarkerIcons.locationPin(
            context = context,
            fillColor = WaxSealRed500.toArgb(),
            ringColor = DeepBrown800.toArgb()
        )
    }
    val pigeonIcon = remember {
        MapMarkerIcons.pigeon(
            context = context,
            bodyColor = DeepBrown800.toArgb(),
            glowColor = GoldAccent400.toArgb()
        )
    }
    val deathIcon = remember {
        MapMarkerIcons.deathCross(
            context = context,
            color = WaxSealRed500.toArgb()
        )
    }

    val originState = rememberMarkerState(position = origin)
    val destinationState = rememberMarkerState(position = destination)
    val pigeonState = rememberMarkerState(position = pigeon)

    // The bird's marker follows every state update, so it visibly crosses the chart.
    LaunchedEffect(origin) { originState.position = origin }
    LaunchedEffect(destination) { destinationState.position = destination }
    LaunchedEffect(pigeon) { pigeonState.position = pigeon }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(midpoint(origin, destination), INITIAL_ZOOM)
    }

    var mapLoaded by remember { mutableStateOf(false) }
    var hasFramedRoute by remember { mutableStateOf(false) }

    // Frame the whole journey once the map is measured, with padding around it.
    LaunchedEffect(mapLoaded, origin, destination) {
        if (!mapLoaded || hasFramedRoute || !hasRealCoordinates(uiState)) return@LaunchedEffect
        hasFramedRoute = true
        runCatching {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngBounds(
                    routeBounds(origin, destination),
                    BOUNDS_PADDING_PX
                )
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = { mapLoaded = true },
        properties = MapProperties(
            mapType = MapType.NORMAL,
            mapStyleOptions = mapStyle,
            isMyLocationEnabled = showMyLocation,
            minZoomPreference = 2f
        ),
        uiSettings = MapUiSettings(
            compassEnabled = false,
            mapToolbarEnabled = false,
            zoomControlsEnabled = false,
            myLocationButtonEnabled = showMyLocation,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false
        )
    ) {
        // Full route: dashed sepia ink.
        Polyline(
            points = samplePath(origin, destination),
            color = DeepBrown700.copy(alpha = 0.75f),
            width = 7f,
            pattern = listOf(Dash(22f), Gap(14f)),
            startCap = RoundCap(),
            endCap = RoundCap()
        )

        // Distance already flown: solid gold.
        Polyline(
            points = samplePath(origin, pigeon),
            color = GoldAccent500,
            width = 11f,
            startCap = RoundCap(),
            endCap = RoundCap()
        )

        Marker(
            state = originState,
            icon = originIcon,
            title = "Dispatched from here",
            anchor = Offset(0.5f, 1f)
        )

        Marker(
            state = destinationState,
            icon = destinationIcon,
            title = "Destination",
            anchor = Offset(0.5f, 1f)
        )

        // A perished pigeon is replaced by a cross at the exact spot where it fell.
        if (uiState.status == MessageStatus.LOST) {
            Marker(
                state = pigeonState,
                icon = deathIcon,
                title = "Here the pigeon perished",
                anchor = Offset(0.5f, 0.5f)
            )
        } else {
            Marker(
                state = pigeonState,
                icon = pigeonIcon,
                title = "Thy pigeon",
                anchor = Offset(0.5f, 0.5f)
            )
        }
    }
}

/**
 * Samples the leg into many small segments. The pigeon's position is interpolated
 * between the two endpoints, so sampling the same interpolation guarantees the bird
 * always sits exactly on the drawn line, and the many segments let the line follow the
 * map's curved Mercator projection instead of being drawn as one straight screen chord.
 */
private fun samplePath(start: LatLng, end: LatLng): List<LatLng> {
    return (0..ROUTE_SAMPLES).map { step ->
        val fraction = step.toDouble() / ROUTE_SAMPLES
        LatLng(
            start.latitude + (end.latitude - start.latitude) * fraction,
            start.longitude + (end.longitude - start.longitude) * fraction
        )
    }
}

/** Halfway point between the two ends of the journey, used for the first camera frame. */
private fun midpoint(origin: LatLng, destination: LatLng): LatLng = LatLng(
    (origin.latitude + destination.latitude) / 2,
    (origin.longitude + destination.longitude) / 2
)

/**
 * Bounds covering the whole journey, widened so a very short hop does not zoom all the
 * way in on a single street.
 */
private fun routeBounds(origin: LatLng, destination: LatLng): LatLngBounds {
    var north = maxOf(origin.latitude, destination.latitude)
    var south = minOf(origin.latitude, destination.latitude)
    var east = maxOf(origin.longitude, destination.longitude)
    var west = minOf(origin.longitude, destination.longitude)

    if (north - south < MIN_BOUNDS_SPAN_DEG) {
        val pad = (MIN_BOUNDS_SPAN_DEG - (north - south)) / 2
        north = (north + pad).coerceAtMost(85.0)
        south = (south - pad).coerceAtLeast(-85.0)
    }
    if (east - west < MIN_BOUNDS_SPAN_DEG) {
        val pad = (MIN_BOUNDS_SPAN_DEG - (east - west)) / 2
        east = (east + pad).coerceAtMost(180.0)
        west = (west - pad).coerceAtLeast(-180.0)
    }

    return LatLngBounds(LatLng(south, west), LatLng(north, east))
}

/** Guards against framing the camera on placeholder (0,0) coordinates. */
private fun hasRealCoordinates(uiState: PigeonMapUiState): Boolean {
    val epsilon = 0.000001
    val senderSet = abs(uiState.senderLat) > epsilon || abs(uiState.senderLng) > epsilon
    val receiverSet = abs(uiState.receiverLat) > epsilon || abs(uiState.receiverLng) > epsilon
    return senderSet || receiverSet
}

/**
 * Small hand-drawn compass rose laid over the corner of the chart.
 */
@Composable
private fun CompassRose(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        drawCircle(color = Parchment100.copy(alpha = 0.85f), radius = radius, center = center)
        drawCircle(
            color = DeepBrown800.copy(alpha = 0.6f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
        // North
        drawLine(
            color = WaxSealRed400,
            start = center,
            end = Offset(center.x, center.y - radius * 0.8f),
            strokeWidth = 3f
        )
        // South
        drawLine(
            color = DeepBrown800,
            start = center,
            end = Offset(center.x, center.y + radius * 0.8f),
            strokeWidth = 2f
        )
        // East / West
        drawLine(
            color = DeepBrown800,
            start = Offset(center.x - radius * 0.8f, center.y),
            end = Offset(center.x + radius * 0.8f, center.y),
            strokeWidth = 2f
        )
        drawCircle(color = GoldAccent500, radius = radius * 0.12f, center = center)
    }
}
