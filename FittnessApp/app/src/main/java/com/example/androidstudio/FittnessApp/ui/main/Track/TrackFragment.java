package com.example.androidstudio.FittnessApp.ui.main.Track;

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


public class TrackFragment extends Fragment implements View.OnClickListener, LocationListener {
    private static final String TAG = "trackFragment";


    //Boolean-Variablen
    private boolean startStopGPS = false;
    private boolean center = true;
    //Buttons
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button centerButton;
    private Button gpsButton;
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
        Log.d(TAG, "onCreateView():  in TrackFragment");

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
                        Log.v(TAG, "going back to home");
                        NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_trackFragment_to_homeFragment);
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
        View view = inflater.inflate(R.layout.track_fragment, container, false);
        geschwindigkeitsView = (TextView) view.findViewById(R.id.kmh);

        //Buttons anlegen und aktivieren
        zoomInButton = (Button) view.findViewById(R.id.zoomIn);
        zoomOutButton = (Button) view.findViewById(R.id.zoomOut);
        centerButton =(Button) view.findViewById(R.id.centerButton);
        gpsButton = (Button) view.findViewById(R.id.gpsButton);
        centerDescription = (TextView) view.findViewById(R.id.centerDescription);
        gpsButtonDescription = (TextView) view.findViewById(R.id.gpsButtonDescription);
        zoomOutButton.setOnClickListener(this);
        zoomInButton.setOnClickListener(this);
        centerButton.setOnClickListener(this);
        gpsButton.setOnClickListener(this);

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
        Log.d(TAG, "onClick():  in TrackFragment");
        switch (view.getId()){
            //Button zum Reinzoomen der Karte
            case R.id.zoomIn:
                Log.d(TAG, "zoomIn-BUtton in TrackFragment");
                mapController.zoomIn(); //Reinzoomen
                break;

            //Button zum Rauszoomen der Karte
            case R.id.zoomOut:
                Log.d(TAG, "zoomOut-Button in TrackFragment");
                mapController.zoomOut(); //Rauszoomen
                break;

            //Button zum zentrieren der Karte
            case R.id.centerButton:
                Log.d(TAG, "centerButton in TrackFragment");
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
                Log.d(TAG,"Start/Stop Button in TrackFragment");
                if ( ! startStopGPS) {
                    Log.d(TAG,"GPS AN");

                    //Starten der Locationupdates wenn Permission gegeben wurde
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, TrackFragment.this);
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
        }
    }
    @Override
    public void onResume() {
        Log.d(TAG, "onResume() in TrackFragment");
        super.onResume();
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause() in TrackFragment");
        super.onPause();
        //Entfernen aller Location-Abfragen und entfernen der Overlays
        mLocationOverlay.disableMyLocation();
        map.getOverlayManager().remove(mLocationOverlay);
        map.getOverlayManager().remove(mCompassOverlay);
        locationManager.removeUpdates(this); //Entfernen der Locationupdates
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onLocationChanged(Location location) {
            Log.v(TAG, "onLocationChanged in TrackFragment");
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
        Log.v(TAG, "getSpeed() in TrackFragment");
        float speed = (location.getSpeed() * 3600 / 1000); //Berechnung der Geschwindigkeit
        String convertedSpeed = String.format("%.2f", speed);
        geschwindigkeitsView.setText(convertedSpeed + "km/h");
    }
}