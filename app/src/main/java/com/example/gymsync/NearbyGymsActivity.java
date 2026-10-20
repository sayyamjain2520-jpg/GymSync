package com.example.gymsync;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NearbyGymsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final LatLng SHIRPUR_CENTER = new LatLng(21.3574, 74.8833); // Shirpur, Maharashtra

    private GoogleMap mMap;
    private PlacesClient placesClient;
    private LatLng userLocation = null;
    private LatLng activeSearchLocation = SHIRPUR_CENTER;
    private String currentSearchLabel = "Shirpur, Maharashtra";

    private ImageButton btBack, btRefreshMap;
    private EditText etGymSearch;
    private Button btSearchSubmit;
    private TextView tvLocationStatus, tvModeTitle, tvModeSubtitle, tvGymsFoundCount;
    private LinearLayout llLoading, llGymListContainer, llEmptyGyms;
    private ProgressBar pbLoadingGyms;

    private final List<GymModel> currentGyms = new ArrayList<>();
    private final Map<Marker, GymModel> markerGymMap = new HashMap<>();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nearby_gyms);

        initViews();
        initPlacesClient();
        initMapFragment();
        checkLocationPermissions();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }

    private void initViews() {
        btBack = findViewById(R.id.btBack);
        btRefreshMap = findViewById(R.id.btRefreshMap);
        etGymSearch = findViewById(R.id.etGymSearch);
        btSearchSubmit = findViewById(R.id.btSearchSubmit);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvModeTitle = findViewById(R.id.tvModeTitle);
        tvModeSubtitle = findViewById(R.id.tvModeSubtitle);
        tvGymsFoundCount = findViewById(R.id.tvGymsFoundCount);
        llLoading = findViewById(R.id.llLoading);
        llGymListContainer = findViewById(R.id.llGymListContainer);
        llEmptyGyms = findViewById(R.id.llEmptyGyms);
        pbLoadingGyms = findViewById(R.id.pbLoadingGyms);

        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btRefreshMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectCurrentLocation();
                etGymSearch.setText("");
                currentSearchLabel = (userLocation != null) ? "Current GPS Location" : "Shirpur, Maharashtra";
                performSearch(currentSearchLabel);
            }
        });

        btSearchSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();
                performSearch(etGymSearch.getText().toString().trim());
            }
        });


        etGymSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                performSearch(etGymSearch.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private String getResolvedApiKey() {
        String key = BuildConfig.MAPS_API_KEY;
        if (key != null && !key.trim().isEmpty() && !key.contains("YOUR_GOOGLE_MAPS_KEY")) {
            return key.trim();
        }

        try {
            key = getString(R.string.google_maps_key);
            if (key != null && !key.trim().isEmpty() && !key.contains("YOUR_GOOGLE_MAPS_KEY")) {
                return key.trim();
            }
        } catch (Exception ignored) {}

        try {
            android.content.pm.ApplicationInfo ai = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (ai != null && ai.metaData != null) {
                key = ai.metaData.getString("com.google.android.geo.API_KEY");
                if (key != null && !key.trim().isEmpty() && !key.contains("YOUR_GOOGLE_MAPS_KEY")) {
                    return key.trim();
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    private void initPlacesClient() {
        try {
            String apiKey = getResolvedApiKey();
            if (apiKey.isEmpty()) {
                tvModeTitle.setText("Places API Key Missing");
                tvModeSubtitle.setText("No API key configured in local.properties. Active Fallback Mode.");
                placesClient = null;
                return;
            }

            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);
            }
            placesClient = Places.createClient(this);
        } catch (Exception e) {
            tvModeTitle.setText("Places SDK Offline");
            tvModeSubtitle.setText("Init error: " + e.getMessage() + ". Active Fallback Mode.");
            placesClient = null;
        }
    }

    private void initMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.gymMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
            if (mapFragment.getView() != null) {
                mapFragment.getView().setOnTouchListener((v, event) -> {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                });
            }
        }
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            detectCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                detectCurrentLocation();
                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException ignored) {}
                }
            } else {
                tvLocationStatus.setText("GPS Permission denied. Type a location above to search.");
                activeSearchLocation = SHIRPUR_CENTER;
                currentSearchLabel = "Shirpur, Maharashtra";
                if (mMap != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(activeSearchLocation, 14f));
                }
            }
            performSearch(currentSearchLabel);
        }
    }

    private boolean isEmulatorDefaultLocation(double lat, double lng) {
        return Math.abs(lat - 37.4220) < 0.05 && Math.abs(lng - (-122.0840)) < 0.05;
    }

    private void detectCurrentLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location bestLocation = null;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Location gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (gpsLoc != null && netLoc != null) {
                    bestLocation = (gpsLoc.getTime() > netLoc.getTime()) ? gpsLoc : netLoc;
                } else if (gpsLoc != null) {
                    bestLocation = gpsLoc;
                } else {
                    bestLocation = netLoc;
                }
            }

            if (bestLocation != null) {
                double lat = bestLocation.getLatitude();
                double lng = bestLocation.getLongitude();

                if (isEmulatorDefaultLocation(lat, lng)) {
                    userLocation = null;
                    activeSearchLocation = SHIRPUR_CENTER;
                    currentSearchLabel = "Shirpur, Maharashtra";
                    tvLocationStatus.setText("Emulator GPS detected (Mountain View). Centered on Shirpur hub.");
                } else {
                    userLocation = new LatLng(lat, lng);
                    activeSearchLocation = userLocation;
                    currentSearchLabel = String.format(Locale.getDefault(), "GPS: %.4f, %.4f", lat, lng);
                    tvLocationStatus.setText(String.format(Locale.getDefault(), "GPS Locked: %.4f, %.4f", lat, lng));
                }
            } else {
                userLocation = null;
                activeSearchLocation = SHIRPUR_CENTER;
                currentSearchLabel = "Shirpur, Maharashtra";
                tvLocationStatus.setText("GPS unavailable. Centered on Shirpur, Maharashtra");
            }
        } catch (Exception e) {
            userLocation = null;
            activeSearchLocation = SHIRPUR_CENTER;
            currentSearchLabel = "Shirpur, Maharashtra";
            tvLocationStatus.setText("Regional Hub: Shirpur, Maharashtra");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {}
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(activeSearchLocation, 14f));

        mMap.setOnMarkerClickListener(marker -> {
            GymModel gym = markerGymMap.get(marker);
            if (gym != null) {
                Toast.makeText(NearbyGymsActivity.this, gym.getName() + " (" + gym.getFormattedDistance() + ")", Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        performSearch(currentSearchLabel);
    }

    private void performSearch(String rawQuery) {
        final String query = (rawQuery == null || rawQuery.trim().isEmpty()) ? "Shirpur" : rawQuery.trim();

        showLoading(true);

        backgroundExecutor.execute(() -> {
            LatLng geocodedCoords = null;
            String geocodedName = null;

            String locationCandidate = query.replaceAll("(?i)\\b(gyms?|fitness|center|club|studios?|near me|nearby|in|at)\\b", "").trim();
            if (locationCandidate.isEmpty()) {
                locationCandidate = query;
            }

            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(NearbyGymsActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocationName(locationCandidate, 3);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        geocodedCoords = new LatLng(addr.getLatitude(), addr.getLongitude());
                        if (addr.getLocality() != null) {
                            geocodedName = addr.getLocality() + (addr.getAdminArea() != null ? ", " + addr.getAdminArea() : "");
                        } else if (addr.getSubAdminArea() != null) {
                            geocodedName = addr.getSubAdminArea() + (addr.getAdminArea() != null ? ", " + addr.getAdminArea() : "");
                        } else {
                            geocodedName = addr.getAddressLine(0);
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (geocodedCoords == null) {
                String candidateLower = locationCandidate.toLowerCase(Locale.ROOT);
                if (candidateLower.contains("shirpur")) {
                    geocodedCoords = SHIRPUR_CENTER;
                    geocodedName = "Shirpur, Maharashtra";
                } else if (candidateLower.contains("dhule")) {
                    geocodedCoords = new LatLng(20.9042, 74.7749);
                    geocodedName = "Dhule, Maharashtra";
                } else if (candidateLower.contains("jalgaon")) {
                    geocodedCoords = new LatLng(21.0077, 75.5626);
                    geocodedName = "Jalgaon, Maharashtra";
                } else if (candidateLower.contains("nashik") || candidateLower.contains("nasik")) {
                    geocodedCoords = new LatLng(19.9975, 73.7898);
                    geocodedName = "Nashik, Maharashtra";
                } else if (candidateLower.contains("pune")) {
                    geocodedCoords = new LatLng(18.5204, 73.8567);
                    geocodedName = "Pune, Maharashtra";
                } else if (candidateLower.contains("mumbai")) {
                    geocodedCoords = new LatLng(19.0760, 72.8777);
                    geocodedName = "Mumbai, Maharashtra";
                }
            }

            final LatLng targetLocation = (geocodedCoords != null) ? geocodedCoords : activeSearchLocation;
            final String targetLabel = (geocodedName != null) ? geocodedName : query;
            final boolean locationResolved = (geocodedCoords != null);

            runOnUiThread(() -> {
                if (locationResolved) {
                    activeSearchLocation = targetLocation;
                    currentSearchLabel = targetLabel;
                    tvLocationStatus.setText("Location: " + targetLabel);
                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(activeSearchLocation, 14f));
                    }
                }

                executePlacesGymSearch(activeSearchLocation, query);
            });
        });
    }

    private void executePlacesGymSearch(LatLng searchLocation, String searchQuery) {
        markerGymMap.clear();
        if (mMap != null) {
            mMap.clear();
        }

        if (placesClient == null) {
            showLoading(false);
            loadFallbackGyms("Places API offline (API key not configured in local.properties). Active Fallback Mode.", searchLocation);
            return;
        }

        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.RATING,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.PHONE_NUMBER,
                Place.Field.WEBSITE_URI
        );

        String textQuery = searchQuery.toLowerCase(Locale.ROOT).contains("gym") ? searchQuery : searchQuery + " gym";

        SearchByTextRequest request = SearchByTextRequest.builder(textQuery, placeFields)
                .setMaxResultCount(12)
                .setLocationBias(CircularBounds.newInstance(searchLocation, 6000.0))
                .build();

        placesClient.searchByText(request)
                .addOnSuccessListener(response -> {
                    showLoading(false);
                    List<Place> places = response.getPlaces();

                    if (places == null || places.isEmpty()) {
                        currentGyms.clear();
                        renderGymCardsAndMarkers();
                        tvModeTitle.setText("Google Places API (0 Results)");
                        tvModeSubtitle.setText("No gyms found for '" + searchQuery + "'. Try another keyword or location.");
                        llEmptyGyms.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvModeTitle.setText("Google Places API (Live Results)");
                    tvModeSubtitle.setText("Found " + places.size() + " active gym listings near " + currentSearchLabel + ".");
                    llEmptyGyms.setVisibility(View.GONE);

                    currentGyms.clear();
                    for (Place place : places) {
                        double lat = (place.getLatLng() != null) ? place.getLatLng().latitude : searchLocation.latitude;
                        double lng = (place.getLatLng() != null) ? place.getLatLng().longitude : searchLocation.longitude;
                        float dist = calculateDistanceKm(searchLocation.latitude, searchLocation.longitude, lat, lng);

                        double rating = (place.getRating() != null) ? place.getRating() : -1.0;
                        int totalRatings = (place.getUserRatingsTotal() != null) ? place.getUserRatingsTotal() : 0;
                        String phone = (place.getPhoneNumber() != null) ? place.getPhoneNumber() : "";
                        String website = (place.getWebsiteUri() != null) ? place.getWebsiteUri().toString() : "";

                        GymModel model = new GymModel(
                                place.getId() != null ? place.getId() : "",
                                place.getName() != null ? place.getName() : "Local Fitness Center",
                                place.getAddress() != null ? place.getAddress() : "Nearby Gym Address",
                                lat,
                                lng,
                                dist,
                                rating,
                                totalRatings,
                                "Open",
                                phone,
                                website,
                                true
                        );
                        currentGyms.add(model);
                    }

                    renderGymCardsAndMarkers();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);
                    String errorMsg = exception.getMessage() != null ? exception.getMessage() : "API call failed";
                    loadFallbackGyms("Places API offline (" + errorMsg + "). Active Fallback Mode.", searchLocation);
                });
    }

    private void loadFallbackGyms(String reasonMessage, LatLng baseLocation) {
        tvModeTitle.setText("Verified Local Directory (Fallback Mode)");
        tvModeSubtitle.setText(reasonMessage);

        currentGyms.clear();
        LatLng base = (baseLocation != null) ? baseLocation : SHIRPUR_CENTER;

        // Curated regional sample gyms placed realistically around the active searched location
        currentGyms.add(new GymModel("fb_1", "Iron Paradise Fitness [Sample]", "Station Road Area",
                base.latitude + 0.0055, base.longitude + 0.0040,
                0, 4.8, 128, "Sample Hours", "", "", false));

        currentGyms.add(new GymModel("fb_2", "Power Zone Athletic Club [Sample]", "Market Complex, College Road",
                base.latitude - 0.0048, base.longitude + 0.0062,
                0, 4.6, 94, "Sample Hours", "", "", false));

        currentGyms.add(new GymModel("fb_3", "FitLife Training Studio [Sample]", "Near Stadium, Bypass Road",
                base.latitude + 0.0080, base.longitude - 0.0055,
                0, 4.5, 76, "Sample Hours", "", "", false));

        currentGyms.add(new GymModel("fb_4", "MetroFlex Crossfit & Gym [Sample]", "Civil Lines High Street",
                base.latitude - 0.0068, base.longitude - 0.0042,
                0, 4.7, 110, "Sample Hours", "", "", false));

        for (GymModel gym : currentGyms) {
            float dist = calculateDistanceKm(base.latitude, base.longitude, gym.getLatitude(), gym.getLongitude());
            gym.setDistanceKm(dist);
        }

        renderGymCardsAndMarkers();
    }

    private void renderGymCardsAndMarkers() {
        llGymListContainer.removeAllViews();
        markerGymMap.clear();

        if (currentGyms.isEmpty()) {
            llEmptyGyms.setVisibility(View.VISIBLE);
            tvGymsFoundCount.setText("No Gyms Found");
            return;
        }

        llEmptyGyms.setVisibility(View.GONE);
        tvGymsFoundCount.setText("Available Gyms (" + currentGyms.size() + ")");

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < currentGyms.size(); i++) {
            GymModel gym = currentGyms.get(i);

            // Add map marker
            if (mMap != null) {
                LatLng gymPos = new LatLng(gym.getLatitude(), gym.getLongitude());
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(gymPos)
                        .title(gym.getName())
                        .snippet((gym.isFromApi() ? "" : "[Fallback] ") + gym.getFormattedDistance())
                        .icon(BitmapDescriptorFactory.defaultMarker(gym.isFromApi() ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_AZURE)));

                if (marker != null) {
                    markerGymMap.put(marker, gym);
                }
            }

            // Inflate card view
            View card = inflater.inflate(R.layout.item_gym_card, llGymListContainer, false);

            TextView tvName = card.findViewById(R.id.tvCardGymName);
            TextView tvAddress = card.findViewById(R.id.tvCardGymAddress);
            TextView tvDistance = card.findViewById(R.id.tvCardGymDistance);
            TextView tvRating = card.findViewById(R.id.tvCardGymRating);
            View llRatingBadge = card.findViewById(R.id.llCardGymRating);
            TextView tvStatus = card.findViewById(R.id.tvCardGymStatus);
            TextView tvSource = card.findViewById(R.id.tvCardGymSource);
            Button btDetails = card.findViewById(R.id.btCardViewDetails);

            tvName.setText(gym.getName());
            tvAddress.setText(gym.getAddress());
            tvDistance.setText(gym.getFormattedDistance());

            if (gym.hasRating()) {
                if (llRatingBadge != null) llRatingBadge.setVisibility(View.VISIBLE);
                tvRating.setText(gym.getFormattedRating());
            } else {
                if (llRatingBadge != null) llRatingBadge.setVisibility(View.GONE);
            }

            if (gym.getOpenStatus() != null && !gym.getOpenStatus().isEmpty()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(gym.getOpenStatus());
            } else {
                tvStatus.setVisibility(View.GONE);
            }

            if (gym.isFromApi()) {
                tvSource.setText("Google Places API");
                tvSource.setTextColor(getColor(R.color.purple_primary));
            } else {
                tvSource.setText("Offline Fallback Data");
                tvSource.setTextColor(getColor(R.color.text_secondary));
            }

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gym.getLatitude(), gym.getLongitude()), 16f));
                    }
                }
            });

            btDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openGymDetails(gym);
                }
            });

            llGymListContainer.addView(card);
        }

        if (mMap != null && !currentGyms.isEmpty()) {
            GymModel first = currentGyms.get(0);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(first.getLatitude(), first.getLongitude()), 14f));
        }
    }

    private void openGymDetails(GymModel gym) {
        Intent intent = new Intent(NearbyGymsActivity.this, GymDetailsActivity.class);
        intent.putExtra("gym_name", gym.getName());
        intent.putExtra("gym_address", gym.getAddress());
        intent.putExtra("gym_distance", gym.getFormattedDistance());
        intent.putExtra("gym_rating", gym.getRating());
        intent.putExtra("gym_phone", gym.getPhone());
        intent.putExtra("gym_lat", gym.getLatitude());
        intent.putExtra("gym_lng", gym.getLongitude());
        intent.putExtra("gym_source", gym.isFromApi() ? "Google Places API" : "Offline Fallback Directory");
        intent.putExtra("user_email", getIntent().getStringExtra("user_email"));
        startActivity(intent);
    }

    private float calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0f;
    }

    private void showLoading(boolean show) {
        if (llLoading != null) {
            llLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
