package com.example.androidstudio.FittnessApp.ui.main.WindSurf;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.example.androidstudio.FittnessApp.R;

import java.util.ArrayList;
import java.util.List;


public class SurfFragment2 extends Fragment implements View.OnClickListener, LocationListener {
    private static final String TAG = "surfFragment2";


    //Boolean-Variablen
    boolean startStopGPS = false;
    boolean center = true;
    //Buttons
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button centerButton;
    private Button gpsButton;
    private Button surf1Button;
    //Overlays
    private CompassOverlay mCompassOverlay; //Kompassoverlay für die Map
    private MyLocationNewOverlay mLocationOverlay; //Anzeige der Position mit einem Pfeil
    //Location,Zeichnen usw.
    LocationManager locationManager;
    private GeoPoint point;
    private IMapController mapController;
    private MapView map;
    private Polyline line; //Linie die hinter dem Positionspfiel gezeichnet wird
    private List<GeoPoint> geoPoints; //Liste der Bewegungspunkte für die Polyline
    private Context ctx;
    //Textviews
    private TextView geschwindigkeitsView;
    private TextView centerDescription;
    private TextView gpsButtonDescription;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView():  in surfFragment2");

        //Abfrage, ob Tracking beendet werden soll (System-Backbutton)
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "onBackPressed");
                AlertDialog.Builder backAlert = new AlertDialog.Builder(getActivity());
                backAlert.setTitle("Tracking Stoppen");
                backAlert.setMessage("Soll das Tracking wirklich gestoppt werden?");
                backAlert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.v(TAG, "going back to surfFragment");
                        NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_surfFragment2_to_action_homeFragment_to_surfFragment);
                    }
                });

                backAlert.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //do nothing
                    }
                });

                backAlert.show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getActivity(), callback);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        View view = inflater.inflate(R.layout.surf_fragment2, container, false);
        geschwindigkeitsView = (TextView) view.findViewById(R.id.kmh);

        //Buttons anlegen und aktivieren
        zoomInButton = (Button) view.findViewById(R.id.zoomIn);
        zoomOutButton = (Button) view.findViewById(R.id.zoomOut);
        centerButton =(Button) view.findViewById(R.id.centerButton);
        gpsButton = (Button) view.findViewById(R.id.gpsButton);
        surf1Button = (Button) view.findViewById(R.id.surf1);
        centerDescription = (TextView) view.findViewById(R.id.centerDescription);
        gpsButtonDescription = (TextView) view.findViewById(R.id.gpsButtonDescription);
        zoomOutButton.setOnClickListener(this);
        zoomInButton.setOnClickListener(this);
        centerButton.setOnClickListener(this);
        gpsButton.setOnClickListener(this);
        surf1Button.setOnClickListener(this);

        ctx = getContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //Map
        map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        //Mapcontroller
        mapController = map.getController();
        mapController.setZoom(8);
        point = new GeoPoint(52.106701, 10.198094);
        mapController.setCenter(point);
        //Compassoverlay
        mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);
        //Polylinevariablen
        geoPoints = new ArrayList<>(); //Liste wird in "onLocationChanged" gefüllt
        line = new Polyline(); //Neue Polyline wird angelegt
        return view;
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick():  in surfFragment2");
        switch (view.getId()){
            //Button zum Reinzoomen der Karte
            case R.id.zoomIn:
                Log.d(TAG, "zoomIn-BUtton in surfFragment2");
                mapController.zoomIn(); //Reinzoomen
                break;

            //Button zum Rauszoomen der Karte
            case R.id.zoomOut:
                Log.d(TAG, "zoomOut-Button in surfFragment2");
                mapController.zoomOut(); //Rauszoomen
                break;

            //Button zum zentrieren der Karte
            case R.id.centerButton:
                Log.d(TAG, "centerButton in surfFragment2");
                if(center) {
                    this.mLocationOverlay.disableFollowLocation(); //Freies Bewegen auf der Karte möglich
                    center = false;
                    centerDescription.setText("Dezentriert");
                } else {
                    this.mLocationOverlay.enableFollowLocation(); //Position wird wieder gefolgt (kein freies Bewegen)
                    centerDescription.setText("Zentriert");
                    center = true;
                }
                break;

            //Button zum starten des Trackings
            case R.id.gpsButton:
                Log.d(TAG,"Start/Stop Button in surfFragment2");
                if ( ! startStopGPS) {
                    Log.d(TAG,"GPS AN");

                    //Starten der Locationupdates wenn Permission gegeben wurde
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                    }

                    //Location-Overlay
                    /* Das Locationoverlay wird erst hier aktiviert, da aus einem mir nicht bekannten Grund,
                     * sobald das Overlay mit "new ..." in der "onCreateView()" angelegt wird, sofort
                     * die Location abgefragt wird, obwohl ".enableMyLocation" oder ".requestLocationUpdates"
                     * nicht aufgerufen werden. */
                    mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx),map);
                    mLocationOverlay.enableMyLocation();
                    mLocationOverlay.enableFollowLocation();
                    map.getOverlays().add(mLocationOverlay);

                    startStopGPS = true;
                    gpsButtonDescription.setText("GPS aktiv");
                    gpsButton.setText("Stop");
                } else {
                    Log.d(TAG,"GPS AUS");
                    locationManager.removeUpdates(this); //Entfernen der Locationupdates

                    mLocationOverlay.disableMyLocation(); /* Muss auch durchgeführt werden, da sonst weiterhin
                     * locations abgefragt werden */

                    map.getOverlayManager().remove(mLocationOverlay); /*Entfernen des Locationoverlays, damit
                     * nicht mehrere übereinander
                     * gezeichnet werden */
                    startStopGPS = false;
                    gpsButtonDescription.setText("GPS inaktiv");
                    gpsButton.setText("Start");
                }
                break;
            case R.id.surf1:
                Log.d(TAG,"Surf 1 Button in surfFragment2");
                Log.d(TAG, "onSurf1Pressed");
                AlertDialog.Builder surf1Alert = new AlertDialog.Builder(getActivity());
                surf1Alert.setTitle("Tracking Stoppen");
                surf1Alert.setMessage("Soll das Tracking wirklich gestoppt werden?");
                surf1Alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.v(TAG, "going back to surfFragment");
                        NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_surfFragment2_to_action_homeFragment_to_surfFragment);
                    }
                });

                surf1Alert.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //do nothing
                    }
                });

                surf1Alert.show();
                break;

        }
    }
    @Override
    public void onResume() {
        Log.d(TAG, "onResume() in surfFragment2");
        super.onResume();
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause() in surfFragment2");
        super.onPause();
        //Entfernen aller Location-Abfragen und entfernen der Overlays
        map.getOverlayManager().remove(mLocationOverlay);
        map.getOverlayManager().remove(mCompassOverlay);
        locationManager.removeUpdates(this); //Entfernen der Locationupdates
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onLocationChanged(Location location) {
        Log.v(TAG, "onLocationChanged in surfFragment2");
        getSpeed(location); //Aufruf der getSpeed-Methode
        point = new GeoPoint(location);
        geoPoints.add(point); //Geopoints werden in die Liste gespeichert

        /*Hier wird kontroliert, ob die Liste mindestens zwei Punkte enthält.
         * Eine Polyline kann nur aus zwei Punkten gezeichnet werden */
        if(geoPoints.size() >= 2) {
            line.setPoints(geoPoints); //Der Linie werden die Geopoints hinzugefügt
            map.getOverlayManager().add(line); //Linie wird gezeichnet
        }
    }

    public void getSpeed(Location location) {
        Log.v(TAG, "getSpeed() in surfFragment2");
        float speed = (location.getSpeed() * 3600 / 1000); //Berechnung der Geschwindigkeit
        String convertedSpeed = String.format("%.2f", speed);
        geschwindigkeitsView.setText(convertedSpeed + "km/h");
    }
}