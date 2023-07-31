package com.tmung.miniminder;

import android.location.Location;
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
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ChildLocFragment extends Fragment {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_loc, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the action bar title
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Child's Phone");
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Get child's last location
                Location location = locationResult.getLastLocation();
                // Update the location in Firebase Realtime Database
                updateLocationInFirebase(location);

            }
        };

        // Get the reference to the child's location in FRD
        databaseRef = FirebaseDatabase.getInstance().getReference("Locs/kid");
        // Initialise database with these coordinates
        /*double childLatitude = 51.404154; // Replace with the actual latitude
        double childLongitude = -0.513871; // Replace with the actual longitude
        databaseRef.child("lat").setValue(childLatitude)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Latitude set successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Latitude NOT set", Toast.LENGTH_SHORT).show();
                });;
        databaseRef.child("long").setValue(childLongitude)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Longitude set successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Longitude NOT set", Toast.LENGTH_SHORT).show();
                });
         */
    }

    // Method to start updating the child's location and sending updates to FRD
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000); // Update interval in milliseconds (10secs)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    // Method to stop updating child's location
    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    // Method to update the child's location in FRD
    private void updateLocationInFirebase(Location location) {
        if (location != null) {
            // Create a data object to store the child's location data in
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("lat", location.getLatitude());
            locationData.put("long", location.getLongitude());

            //Toast.makeText(getActivity(), "updateLocationInFirebase", Toast.LENGTH_SHORT).show();

            // Update the child's location in the "child_location" node in FRD
            databaseRef.setValue(locationData)
                    .addOnSuccessListener(aVoid -> {
                        // Location update in Firebase successful
                        Toast.makeText(getActivity(), "Location update in FRD successful", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure of location update
                        Log.e("LOCATIONUPDATE", e.getMessage());
                        Toast.makeText(getActivity(), "Failed to update location in FRD", Toast.LENGTH_SHORT).show();
                    });
        }
    }
    /*private void updateLocationInFirebase(Location location) {
        if (location != null) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            Toast.makeText(getActivity(), "updateLocationInFirebase", Toast.LENGTH_SHORT).show();

            // Update the child's location in the "child_location" node in FRD
            databaseRef.setValue(lat)
                    .addOnSuccessListener(aVoid -> {
                        // Location update in Firebase successful
                        Toast.makeText(getActivity(), "Lat update in FRD successful", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("UPDATE LONDATA", "Error updating", e);
                        }
                    });
            databaseRef.setValue(lon)
                    .addOnSuccessListener(aVoid -> {
                        // Location update in Firebase successful
                        Toast.makeText(getActivity(), "Lon update in FRD successful", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("UPDATE LONDATA", "Error updating", e);
                        }
                    });
        }
    }
     */

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}

