package com.tmung.miniminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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

public class ParentMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private Marker childMarker;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_maps);

        // Initialise the map
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // set up Geofencing
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofencePendingIntent = createGeofencingPendingIntent();
        setupGeofence();
    } // end of onCreate

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    // Create a PendingIntent for the GeofenceBroadcastReceiver
    private PendingIntent createGeofencingPendingIntent() {
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofences were added successfully
                            Toast.makeText(ParentMapsActivity.this, "Geofence added successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Geofences failed to be added
                            Toast.makeText(ParentMapsActivity.this, "Failed to set up geofence", Toast.LENGTH_SHORT).show();

                        }
                    });
        }
    }

    // update the map with the child's location
    private void updateChildLocOnMap(Location location) {
        if (googleMap != null) {
            LatLng childLatLng = new LatLng(location.getLatitude(), location.getLongitude());

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


