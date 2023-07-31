package com.tmung.miniminder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class HomeFragment extends Fragment {

    private LinearLayout  mBottomSheetLayout;
    private BottomSheetBehavior sheetBehavior;
    private ImageView header_arrow_image;

    Button btn_addNewGeofence;
    Button btn_viewActiveGeofences;
    Button btn_viewLocationLogs;

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

        // Find the button in our view and set an OnClickListener to it
        btn_viewActiveGeofences = view.findViewById(R.id.btn_viewActiveGeofences);
        btn_viewActiveGeofences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // when clicked, show BottomSheet
                InteractiveBottomSheetDialogFragment bottomSheetFragment = InteractiveBottomSheetDialogFragment.newInstance();
                bottomSheetFragment.show(getParentFragmentManager(), "Custom Bottom Sheet");
            }
        });

        mBottomSheetLayout = view.findViewById(R.id.bottom_sheet_layout);
        sheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout);
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
    }
}
