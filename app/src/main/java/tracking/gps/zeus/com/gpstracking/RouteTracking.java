package tracking.gps.zeus.com.gpstracking;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;


public class RouteTracking extends FragmentActivity implements LocationListener, LocationSource {

    private static final String TAG = RouteTracking.class.getSimpleName();

    private static final long   MIN_TIME_BW_UPDATES = 5000;
    private static final float  MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int TEN_SECONDS = 1000 * 10;

    private GoogleMap       googleMap;
    private LocationManager locationManager;

    private OnLocationChangedListener mListener;


    private Location        currentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_tracking);

        initMap();


    }

    //LocationListener
    @Override
    public void onLocationChanged(Location location) {


        if( mListener != null ){

            if( isBetterLocation(location, currentLocation) ){
                currentLocation = location;

                mListener.onLocationChanged(location);

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()),17));

                Toast.makeText(this, "New Location -> Provider: " + location.getProvider()+", Latlng:"+new LatLng(location.getLatitude(), location.getLongitude()).toString(), Toast.LENGTH_LONG).show();
            }
        }

        Toast.makeText(this, "cambio la ubicacion... ", Toast.LENGTH_LONG).show();

    }

    //LocationListener
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    //LocationListener
    @Override
    public void onProviderEnabled(String provider) {

    }

    //LocationListener
    @Override
    public void onProviderDisabled(String provider) {

    }

    /** Metodo que se encarga de inicializar el mapa**/
    private void initMap(){

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if( locationManager != null ){
            locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this );
            locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this );
        }


        if( googleMap == null ){

            /** Obteniendo el mapa desde el SupportMapFragment**/
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

            if( googleMap != null){
                /** Haabilitando la localización **/
                googleMap.setMyLocationEnabled(true);

                /** Habilitando los controles para el zoom **/
                googleMap.getUiSettings().setZoomControlsEnabled(true);

                /** Deshabilitando el boton de myLocation **/
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);

                googleMap.setLocationSource(this);



            }

        }

    }


    private void getLocation(){

        String bestProviderName = getProviderName();

        try{

            locationManager.requestLocationUpdates( bestProviderName, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this );

        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     * Metodo que se encarga de obtener el mejor Provider de acuerdo al Criteria
     * @return Nombre del mejor provider según la situación
     * */
    private String getProviderName() {
        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW); // Chose your desired power consumption level.
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // Choose your accuracy requirement.
        criteria.setSpeedRequired(true); // Chose if speed for first location fix is required.
        criteria.setAltitudeRequired(false); // Choose if you use altitude.
        criteria.setBearingRequired(false); // Choose if you use bearing.
        criteria.setCostAllowed(false); // Choose if this provider can waste money :-)

        // Provide your criteria and flag enabledOnly that tells
        // LocationManager only to return active providers.

        String bestProvider = locationManager.getBestProvider(criteria, true);

        Log.d(TAG, "Provider: " + bestProvider);
        Toast.makeText(this, "Provider: " + bestProvider, Toast.LENGTH_LONG).show();

        return bestProvider;
    }

    //LocationSource
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;

    }

    //LocationSource
    @Override
    public void deactivate() {
        mListener = null;

    }



    /**
     * Determines whether one Location reading is better than the current Location
     * fix. This is the sample code used in the android documentation.
     *
     * @param location The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to
     *     compare the new one.
     */
    protected boolean isBetterLocation(Location location,
                                       Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        if (timeDelta < TEN_SECONDS) {
            return false;
        }

        // If it's been more than two minutes since the current location, use the
        // new location because the user has likely moved.
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
                .getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        float distance = location.distanceTo(currentBestLocation);

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate && distance > 10) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
