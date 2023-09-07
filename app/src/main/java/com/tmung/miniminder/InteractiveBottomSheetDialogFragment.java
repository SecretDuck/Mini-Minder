package com.tmung.miniminder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class InteractiveBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static InteractiveBottomSheetDialogFragment newInstance() {
        return new InteractiveBottomSheetDialogFragment();
    }

    // class for the sliding bottom sheet
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_sheet, container, false);

        // only inflation necessary

        return view;
    }
}

