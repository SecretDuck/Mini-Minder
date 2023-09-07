package com.tmung.miniminder;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimulationFragment extends Fragment implements OnMapReadyCallback {

    // declare class-level variables
    private LinearLayout bottomSheetLayout;
    private BottomSheetBehavior sheetBehavior;
    private ImageView header_arrow_image;
    private GoogleMap googleMap;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private Marker childMarker;
    private DatabaseReference databaseRef, simulPathRef, geofenceRef;
    private boolean isSimulRunning = false;
    private Handler simulHandler = new Handler();
    private Runnable simulRunnable;
    private List<LocationData> existingGeofences;
    private List<LocationData> locationDataList;
    private Map<LatLng, Circle> circles;
    private ValueEventListener valueEventListener;
    private double geofenceLat, geofenceLong;
    private float geofenceRadius = 110;
    private Marker initialMarker;
    private boolean isChildInGeofence = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simulation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        existingGeofences = new ArrayList<>(); // to store the geofence(s) for simulation
        circles = new HashMap<>(); // to store the geofence circle for simulation
        locationDataList = new ArrayList<>(); // to store the simulation data points

        // Set the action bar title
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Simulation");
        }

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // set up Geofencing
        geofencingClient = LocationServices.getGeofencingClient(requireActivity());
        geofencePendingIntent = createGeofencingPendingIntent();

        bottomSheetLayout = view.findViewById(R.id.sim_bottom_sheet_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        // Set the initial state to expanded
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        header_arrow_image = view.findViewById(R.id.bottom_sheet_arrow);
        header_arrow_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {}

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                header_arrow_image.setRotation(slideOffset * 180);
            }
        });

        LinearLayout startSimulation = bottomSheetLayout.findViewById(R.id.startSimulation);
        LinearLayout stopSimulation = bottomSheetLayout.findViewById(R.id.stopSimulation);

        simulPathRef = FirebaseDatabase.getInstance().getReference("simulationPath");
        startSimulation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // remove initialMarker if it exists
                if (initialMarker != null) {
                    initialMarker.remove();
                    initialMarker = null;
                }

                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                isSimulRunning = true;
                Toast.makeText(requireContext(), "Simulation running", Toast.LENGTH_LONG).show();

                valueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                        simulRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (iterator.hasNext() && isSimulRunning) {
                                    DataSnapshot snapshot = iterator.next();
                                    LocationData locationData = snapshot.getValue(LocationData.class);
                                    if (locationData != null) {
                                        updateChildLocOnMap(locationData.getLatitude(), locationData.getLongitude());
                                    }
                                    // Re-run this runnable after 2 seconds
                                    simulHandler.postDelayed(this, 1000);
                                } else {
                                    isSimulRunning = false;
                                    simulHandler.removeCallbacks(simulRunnable);
                                }
                            }
                        };
                        // Initial run
                        simulHandler.post(simulRunnable);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle database error
                        Log.e("Firebase", "onCancelled", databaseError.toException());
                    }
                };
                simulPathRef.addListenerForSingleValueEvent(valueEventListener);
            }
        });
        stopSimulation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(), "Simulation stopped", Toast.LENGTH_SHORT).show();
                isSimulRunning = false;
                // Remove event listener
                simulPathRef.removeEventListener(valueEventListener);
            }
        });

    } // end onViewCreated here

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        // Load up pre-saved simulation locations & place initial marker
        getSimulationLocations();

        // onMapClick will check if geofence exists there and delete it
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                if (isSimulRunning) { // don't allow deleting geofence when simulation is running
                    if (!existingGeofences.isEmpty()) {
                        Toast.makeText(getActivity(), "Please end simulation to delete geofence", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // check that list is not empty
                    if (existingGeofences.size() > 0) {
                        LocationData locationData = existingGeofences.get(0); // since there's only one value in simulation

                        // Calculate the distance between clicked point and geofence centre
                        LatLng geofenceCenter = new LatLng(locationData.getLatitude(), locationData.getLongitude());
                        double distance = SphericalUtil.computeDistanceBetween(geofenceCenter, latLng);

                        // check if clicked point is inside geofence radius
                        if (distance <= geofenceRadius) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Geofence")
                                    .setMessage("Do you want to delete this geofence?")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Remove circle
                                            Circle circle = circles.get(geofenceCenter);
                                            if (circle != null) {
                                                circle.remove();
                                                circles.remove(geofenceCenter);
                                            }
                                            // Clear the existingGeofences list
                                            existingGeofences.clear();
                                            // Reset the geofence latitude and longitude
                                            geofenceLat = 0;
                                            geofenceLong = 0;
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    }
                }
            }
        });

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                // Create confirmation dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Geofence")
                        .setMessage("Create geofence here?")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // if confirmed, setup geofence at loc, send geofence to database
                                // existingGeofences is also updated by setupGeofence
                                setupGeofence(latLng.latitude, latLng.longitude);
                                geofenceLat = latLng.latitude;
                                geofenceLong = latLng.longitude;
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    } // end onMapReady

    // Method to check if a geofence already exists
    private boolean geofenceExists(double latitude, double longitude) {
        for (LocationData geofence : existingGeofences) {
            if (geofence.getLatitude() == latitude && geofence.getLongitude() == longitude) {
                return true;
            }
        }
        return false;
    }

    public void getSimulationLocations() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = database.getReference("simulationPath");
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                locationDataList.clear(); // Clear the list to avoid duplication
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    LocationData locationData = ds.getValue(LocationData.class);
                    locationDataList.add(locationData);
                }
                // Check if the list is not empty and plot the first location
                if (!locationDataList.isEmpty() && locationDataList.get(0) != null) {
                    LocationData firstLocation = locationDataList.get(0);
                    LatLng firstLatLng = new LatLng(firstLocation.getLatitude(), firstLocation.getLongitude());

                    // remove the old marker
                    if (initialMarker != null) {
                        initialMarker.remove();
                    }
                    // Place initial marker and reference it marker to make it easy to delete
                    initialMarker = googleMap.addMarker(new MarkerOptions().position(firstLatLng).title("First Location"));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, 14));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(requireActivity(), "Failed to read value", Toast.LENGTH_SHORT).show();
            }
        };
        dbRef.addValueEventListener(valueEventListener);
    }

    // Method to fetch existing geofences from Firebase, will be called on fragment start
    private void fetchExistingGeofences(GeofenceFetcher geofenceFetcher) {
        geofenceRef = FirebaseDatabase.getInstance().getReference("geofenceLocations");
        geofenceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<LocationData> geofenceLocations = new ArrayList<>();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    LocationData locationData = postSnapshot.getValue(LocationData.class);
                    if (locationData != null) {
                        locationData.setId(postSnapshot.getKey()); // set the key
                        geofenceLocations.add(locationData);
                    }
                }
                geofenceFetcher.onFetched(geofenceLocations);  // Call the fetcher when done
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "onCancelled", databaseError.toException());
            }
        });
    }

    // Create a PendingIntent for the GeofenceBroadcastReceiver
    private PendingIntent createGeofencingPendingIntent() {
        Intent intent = new Intent(getActivity(), GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    // Method to create a geofence
    private void setupGeofence(double latitude, double longitude) {

        // check if geofence already exists
        if (geofenceExists(latitude, longitude)) {
            Toast.makeText(getContext(), "Geofence already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        // build the geofence
        Geofence geofence = new Geofence.Builder()
                .setRequestId("child_geofence") // Unique ID for the geofence
                .setCircularRegion(latitude, longitude, geofenceRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();

        // ask for location permissions first
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofence was added successfully
                            // add geofence's data to existingGeofences
                            existingGeofences.add(new LocationData(latitude, longitude, geofenceRadius));
                        }
                    })
                    .addOnFailureListener(requireActivity(), new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Geofences failed to be added
                            Toast.makeText(getContext(), "Failed to set up geofence", Toast.LENGTH_SHORT).show();
                            Log.e("Geofence Error", e.getMessage());
                        }
                    });
            // Draw circle around the geofence
            Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(new LatLng(latitude, longitude))
                    .radius(geofenceRadius) // draw the geofence with the radius specified
                    .strokeColor(Color.RED)
                    .fillColor(0x220000FF)
                    .strokeWidth(5));
            circles.put(new LatLng(geofence.getLatitude(), geofence.getLongitude()), circle);
        } else {
            Toast.makeText(requireActivity(), "Permissions not given", Toast.LENGTH_SHORT).show();
        }
    }

    // Update the map with the child's location
    private void updateChildLocOnMap(double latitude, double longitude) {
        if (googleMap != null) {
            LatLng childLatLng = new LatLng(latitude, longitude);

            // remove the previous marker, if any
            if (childMarker != null) {
                childMarker.remove();
            }

            // customise the marker for the child's loc
            //Bitmap childIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.child_icon);
            //BitmapDescriptor childIcon = BitmapDescriptorFactory.fromBitmap(childIconBitmap);

            // add a marker for the child's location
            childMarker = googleMap.addMarker(new MarkerOptions()
                    .position(childLatLng)
                    //.icon(childIcon)
                    .title("Child's Location"));

            // move the camera to the child's location with an appropriate zoom level
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(childLatLng, 16));

            // Manually checking distance with the geofence's center
            LatLng geofenceCenter = new LatLng(geofenceLat, geofenceLong);
            double distanceToGeofence = SphericalUtil.computeDistanceBetween(childLatLng, geofenceCenter);

            if (distanceToGeofence <= geofenceRadius) {
                // child is now inside geofence, but isChildInGeofence says they previously weren't
                if (!isChildInGeofence) {
                    // Child just entered the geofence
                    showNotification("Mini-Minder Geofence", "Child has entered the geofence.");
                    isChildInGeofence = true; // update status
                }
            } else { // child is now outside geofence but isChildInGeofence says they previously were
                if (isChildInGeofence) {
                    // Child just exited the geofence
                    showNotification("Mini-Minder Geofence", "Child has left the geofence.");
                    isChildInGeofence = false; // update status
                }
            }
        }
    }

    // Method for simulation to display notification for geofence transition (enter/exit)
    private void showNotification(String title, String content) {
        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel("200",
                "Mini-Minder Geofence",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Geofence triggered");
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), "200")
                .setSmallIcon(R.drawable.ic_notification) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(content)// message for notification
                .setAutoCancel(true); // clear notification after click

        notificationManager.notify(0, builder.build());
    }

}
