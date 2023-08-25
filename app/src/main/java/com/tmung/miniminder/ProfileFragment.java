package com.tmung.miniminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    // LinearLayout variable for the profile layout
    private LinearLayout profileLayout;
    private FirebaseAuth firebaseAuth;

    // Inflate the layout for this fragment
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    // Once view is created, perform some actions
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the action bar title
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Profile");
        }

        // Initialise the profileLayout variable with the corresponding view
        profileLayout = view.findViewById((R.id.profile_menu));

        // Find the sub-elements within the profile layout
        LinearLayout viewLinkedAccounts = profileLayout.findViewById(R.id.viewLinkedAccounts);
        LinearLayout linkNewAccount = profileLayout.findViewById(R.id.linkNewAccount);
        LinearLayout manageChildrensProf = profileLayout.findViewById(R.id.manageChildrensProf);
        LinearLayout profileLogout = profileLayout.findViewById(R.id.profile_logout);

        // Set onClickListeners for the buttons below
        viewLinkedAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireActivity(), "Will show linked accounts", Toast.LENGTH_SHORT).show();
            }
        });
        linkNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireActivity(), "Will show linked accounts", Toast.LENGTH_SHORT).show();
            }
        });
        manageChildrensProf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireActivity(), "Will show linked accounts", Toast.LENGTH_SHORT).show();
            }
        });
        // Method to log user out of app
        profileLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signOut();

                // After logging out, navigate back to the LoginActivity
                if (getActivity() != null) { // make sure fragment is attached to an activity
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    getActivity().finish();
                }
            }
        });
    }
}
