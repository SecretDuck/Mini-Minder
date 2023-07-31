package com.tmung.miniminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ParentMapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private Marker childMarker;
    private DatabaseReference databaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the action bar title
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Parent's Map");
        }

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // set up Geofencing
        geofencingClient = LocationServices.getGeofencingClient(requireActivity());
        geofencePendingIntent = createGeofencingPendingIntent();
        setupGeofence();

        // Get the reference to the child's location in Firebase Realtime Database
        databaseRef = FirebaseDatabase.getInstance().getReference("Locs/kid");

        //FirebaseApp.initializeApp(requireActivity());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(51.404154, -0.513871);
        googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15));
         */

        // Start listening for child's location updates from Firebase Realtime Database
        listenForChildLocationUpdates();
    }

    // Method to listen for child's location updates from Firebase Realtime Database
    private void listenForChildLocationUpdates() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the child's location data from Firebase
                Double latitude = dataSnapshot.child("lat").getValue(Double.class);
                Double longitude = dataSnapshot.child("long").getValue(Double.class);

                if (latitude != null && longitude != null) {
                    // Update the map with the child's location
                    Toast.makeText(getActivity(), "lat, long NOT null: " + latitude + " , " + longitude, Toast.LENGTH_LONG).show();
                    updateChildLocOnMap(latitude, longitude);
                } else {
                    updateChildLocOnMap(34.0195, 118.4912);
                    Toast.makeText(getActivity(), "Lat and Long are NULL; but listening...", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database read error (if any)
                Toast.makeText(getActivity(), "Database-read error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Create a PendingIntent for the GeofenceBroadcastReceiver
    private PendingIntent createGeofencingPendingIntent() {
        Intent intent = new Intent(getActivity(), GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    // Set up the geofence around the child's location (currently Thorpe Park)
    private void setupGeofence() {
        // Get the child's location from the server or cloud database
        double childLatitude = 51.404154; // (Thorpe Park) Replace with the child's actual latitude
        double childLongitude = -0.513871; // Replace with the child's actual longitude
        float geofenceRadius = 100; // Geofence radius in METERS

        Geofence geofence = new Geofence.Builder()
                .setRequestId("child_geofence") // Unique ID for the geofence
                .setCircularRegion(childLatitude, childLongitude, geofenceRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();
        // default Toast to check permissions check
        //Toast.makeText(requireActivity(), "setupGeofence working...", Toast.LENGTH_SHORT).show();

        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofences were added successfully
                            Toast.makeText(getContext(), "Geofence added successfully", Toast.LENGTH_SHORT).show();
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
        } else {
            Toast.makeText(requireActivity(), "Permissions not given", Toast.LENGTH_SHORT).show();
        }
    }


    // update the map with the child's location
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
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(childLatLng, 15));
        }
    }
}
