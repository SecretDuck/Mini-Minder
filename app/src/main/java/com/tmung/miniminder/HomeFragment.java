package com.tmung.miniminder;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Pair;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class HomeFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    // TODO: incorporate ViewModel to separate the UI and activity/fragment
    private LinearLayout  mBottomSheetLayout;
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
    private List<LocationData> existingGeofences = new ArrayList<>();
    private Map<LatLng, Circle> circles = new HashMap<>();
    private SharedViewModel sharedViewModel;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the action bar title
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Home");
        }

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // set up Geofencing
        geofencingClient = LocationServices.getGeofencingClient(requireActivity());
        geofencePendingIntent = createGeofencingPendingIntent();

        // Get the reference to the child's location in Firebase Realtime Database
        databaseRef = FirebaseDatabase.getInstance().getReference("encryptedLocation");

        mBottomSheetLayout = view.findViewById(R.id.bottom_sheet_layout);
        sheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout);
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

        LinearLayout viewCurrLL = mBottomSheetLayout.findViewById(R.id.viewCurrLinearLayout);
        LinearLayout addNewGeofLL = mBottomSheetLayout.findViewById(R.id.addNewGeofLinearLayout);
        LinearLayout simulateChild = mBottomSheetLayout.findViewById(R.id.simulateChild);
        LinearLayout viewLocLogsLL = mBottomSheetLayout.findViewById(R.id.viewLocLogsLinearLayout);
        LinearLayout manageChildrensProfLL = mBottomSheetLayout.findViewById(R.id.manageChildrensProf);

        viewCurrLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Showing current geofences on map", Toast.LENGTH_SHORT).show();

                fetchExistingGeofences(new GeofenceFetcher() {
                    @Override
                    public void onFetched(List<LocationData> geofenceLocations) {
                        googleMap.clear();  // Clear all existing markers
                        existingGeofences = geofenceLocations;  // Update the existing geofences

                        // Draw circle around each geofence
                        for (LocationData geofence : geofenceLocations) {
                            googleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(geofence.getLatitude(), geofence.getLongitude()))
                                    .title("Geofence Location"));

                            Circle circle = googleMap.addCircle(new CircleOptions()
                                    .center(new LatLng(geofence.getLatitude(), geofence.getLongitude()))
                                    .radius(150)
                                    .strokeColor(Color.RED)
                                    .fillColor(0x220000FF)
                                    .strokeWidth(5));
                            circles.put(new LatLng(geofence.getLatitude(), geofence.getLongitude()), circle);
                        }
                    }
                });
            }
        });
        addNewGeofLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Might allow to add new geofences", Toast.LENGTH_SHORT).show();
            }
        });
        simulPathRef = FirebaseDatabase.getInstance().getReference("simulationPath");
        simulateChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireActivity(), "Please switch to simulation tab", Toast.LENGTH_SHORT).show();
            }
        });

        viewLocLogsLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Will show location logs", Toast.LENGTH_SHORT).show();
            }
        });
        manageChildrensProfLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Will allow to manage children's profiles", Toast.LENGTH_SHORT).show();
            }
        });

    } // end onViewCreated here

    /*@Override
    public void onDestroyView() {
        super.onDestroyView();

        if (geofenceLocationsRef != null && valueEventListener != null) {
            geofenceLocationsRef.removeEventListener(valueEventListener);
        }
    }
     */

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Start listening for child's location updates from Firebase Realtime Database
        listenForChildLocationUpdates();
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                fetchExistingGeofences(new GeofenceFetcher() {
                    @Override
                    public void onFetched(List<LocationData> geofenceLocations) {
                        for (LocationData locationData : geofenceLocations) {
                            //if (locationData.getLatitude() == latLng.latitude && locationData.getLongitude() == latLng.longitude) {
                            if (geofenceExists(latLng.latitude, latLng.longitude)) {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Delete Geofence")
                                        .setMessage("Do you want to delete this geofence?")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // User clicked "Yes", delete the geofence
                                                DatabaseReference geofenceRef = FirebaseDatabase.getInstance().getReference("geofenceLocations");
                                                geofenceRef.child(locationData.getId()).removeValue();
                                                // Remove circle
                                                Circle circle = circles.get(latLng);
                                                if (circle != null) {
                                                    circle.remove();
                                                    circles.remove(latLng);
                                                }
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, null)
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                                return; // Found the matching geofence, no need to continue
                            }
                        }
                    }
                });
            }
        });
        // Method for the simulation; sends coordinates to firebase to simulate child moving
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                // Create confirmation dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Geofence")
                        .setMessage("Create geofence here?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // if confirmed, setup geofence at loc, send geofence to database
                                setupGeofence(latLng.latitude, latLng.longitude);
                                sendGeofenceToFirebase(latLng.latitude, latLng.longitude);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        //setupGeofenceChildCurrLoc();
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

    private void sendGeofenceToFirebase(double latitude, double longitude) {
        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("geofenceLocations");

        // Create a new LocationData object
        LocationData locationData = new LocationData(latitude, longitude);

        // Use push().setValue to create a new child with a unique ID in your table
        myRef.push().setValue(locationData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(), "Geofence data sent to Firebase", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failed to send location data to Firebase", Toast.LENGTH_SHORT).show();
                        Log.e("Firebase Error", e.getMessage());
                    }
                });
    }


    // Retrieve lat and long of marker when clicked, and setup geofence there
    @Override
    public boolean onMarkerClick(final Marker marker) {
        // Get LatLng from marker
        final LatLng markerPos = marker.getPosition();

        fetchExistingGeofences(new GeofenceFetcher() {
            @Override
            public void onFetched(List<LocationData> geofenceLocations) {
                for (LocationData locationData : geofenceLocations) {
                    if (locationData.getLatitude() == markerPos.latitude && locationData.getLongitude() == markerPos.longitude) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Geofence")
                                .setMessage("Do you want to delete this geofence?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // User clicked "Yes", delete the geofence
                                        DatabaseReference geofenceRef = FirebaseDatabase.getInstance().getReference("geofenceLocations");
                                        geofenceRef.child(locationData.getId()).removeValue();
                                        marker.remove();
                                        // Also remove circle
                                        Circle circle = circles.get(markerPos);
                                        if (circle != null) {
                                            circle.remove();
                                            circles.remove(markerPos);
                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        return; // Found the matching geofence, no need to continue
                    }
                }
            }
        });

        return true;
    }

    // Method to check if a geofence already exists
    private boolean geofenceExists(double latitude, double longitude) {
        for (LocationData geofence : existingGeofences) {
            if (geofence.getLatitude() == latitude && geofence.getLongitude() == longitude) {
                return true;
            }
        }
        return false;
    }

    // Method to listen for child's location updates from Firebase Realtime Database
    /*
    private void listenForChildLocationUpdates() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get the child's location data from Firebase
                Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                Double longitude = dataSnapshot.child("longitude").getValue(Double.class);

                if (latitude != null && longitude != null) {
                    // Update the map with the child's location
                    Toast.makeText(getActivity(), "Starting...", Toast.LENGTH_LONG).show();
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
     */
    private void listenForChildLocationUpdates() {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getAesKey().observe(getViewLifecycleOwner(), aesKey -> {
            databaseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Assuming the latitude and longitude are encrypted, decrypt them here using aesKey
                    String encryptedLocation = dataSnapshot.child("encryptedLocation").getValue(String.class);

                    Pair<Double, Double> latitudeLongitude;
                    try {
                        latitudeLongitude = decrypt(encryptedLocation, aesKey); // You will need to define this method
                    } catch (Exception e) {
                        // handle the exception
                        Log.e("DecryptionError", "Error decrypting data", e);
                        return;
                    }

                    if (latitudeLongitude != null) {
                        // Update the map with the child's location
                        Toast.makeText(getActivity(), "Starting...", Toast.LENGTH_LONG).show();
                        updateChildLocOnMap(latitudeLongitude.first, latitudeLongitude.second);
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
        });
    }

    // EXPLAIN THIS
    public static Pair<Double, Double> decrypt(String cipherText, SecretKey aesKey) throws Exception {
        byte[] ivAndCiphertext = Base64.getDecoder().decode(cipherText);

        // Extract the IV and ciphertext from the input string
        byte[] iv = new byte[16];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, iv.length);
        byte[] encryptedBytes = new byte[ivAndCiphertext.length - iv.length];
        System.arraycopy(ivAndCiphertext, iv.length, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // Parse the decrypted string back to doubles
        String[] latLon = new String(decryptedBytes, StandardCharsets.UTF_8).split(",");
        return new Pair<>(Double.parseDouble(latLon[0]), Double.parseDouble(latLon[1]));
    }

    // Create a PendingIntent for the GeofenceBroadcastReceiver
    private PendingIntent createGeofencingPendingIntent() {
        Intent intent = new Intent(getActivity(), GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    // Set up the geofence around the child's location (currently Thorpe Park)
    private void setupGeofence(double latitude, double longitude) {
        // Get the child's location from the server or cloud database
        //double childLatitude = 51.404154; // (Thorpe Park) Replace with the child's actual latitude
        //double childLongitude = -0.513871; // Replace with the child's actual longitude
        float geofenceRadius = 150; // Geofence radius in METERS

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
            // Draw circle around the geofence
            /*CircleOptions circleOptions = new CircleOptions()
                    .center(new LatLng(latitude, longitude))
                    .radius(geofenceRadius)
                    .strokeColor(Color.RED)
                    .fillColor(0x220000FF)
                    .strokeWidth(5);
            googleMap.addCircle(circleOptions);
             */
            childMarker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    //.icon(childIcon)
                    .title("Child's Location"));
            Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(new LatLng(latitude, longitude))
                    .radius(150)
                    .strokeColor(Color.RED)
                    .fillColor(0x220000FF)
                    .strokeWidth(5));
            circles.put(new LatLng(geofence.getLatitude(), geofence.getLongitude()), circle);
        } else {
            Toast.makeText(requireActivity(), "Permissions not given", Toast.LENGTH_SHORT).show();
        }

    }

    // Setup geofence at child's current loc from Firebase
    private void setupGeofenceChildCurrLoc() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("encryptedLocation");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double latitude = snapshot.child("lat").getValue(Double.class);
                Double longitude = snapshot.child("long").getValue(Double.class);

                if (latitude != null && longitude != null) {
                    setupGeofence(latitude, longitude);
                } else {
                    Toast.makeText(getActivity(),"Child's current location not found in database", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Error while reading database", Toast.LENGTH_SHORT).show();
            }
        });
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
            //Bitmap childIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_child);
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
