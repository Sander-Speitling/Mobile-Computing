package com.example.androidstudio.FittnessApp.ui.main.Home;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidstudio.FittnessApp.R;

public class HomeFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "HomeFragment";
    // Cardviews
    CardView trackCard;
    CardView surfCard;
    CardView settingsCard;
    CardView cardioCard;
    CardView bicycleCard;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView():  ");

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        // Zuweisung der entsprechenden Views
        trackCard = (CardView) view.findViewById(R.id.trackCard);
        settingsCard = (CardView) view.findViewById(R.id.settingsCard);
        cardioCard = (CardView) view.findViewById(R.id.cardioCard);
        bicycleCard= (CardView) view.findViewById(R.id.bicycleCard);
        surfCard= (CardView) view.findViewById(R.id.surfCard);

        // Onclicklistener
        trackCard.setOnClickListener(this);
        surfCard.setOnClickListener(this);
        settingsCard.setOnClickListener(this);
        cardioCard.setOnClickListener(this);
        bicycleCard.setOnClickListener(this);

        return view;
    }



    // Löst die entsprechende Aktion zum Wechseln zu den verschiednene Fragments aus
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.trackCard:
                Log.v(TAG, "trackcard was clicked");
                NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_trackFragment);
                break;
            case R.id.settingsCard:
                Log.v(TAG, "settingsCard was clicked");
                NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_settingsFragment);
                break;
            case R.id.cardioCard:
                Log.v(TAG, "cardioCard was clicked");
                NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_cardioFragment);
                break;
            case R.id.bicycleCard:
                Log.v(TAG, "bicycleCard was clicked");
                NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_bikeRunFragment);
                break;
            case R.id.surfCard:
                Log.v(TAG, "surfCard was clicked");
                NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_surfFragment);
                break;


        }
    }
}