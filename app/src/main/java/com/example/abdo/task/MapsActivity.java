package com.example.abdo.task;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.abdo.task.Common.Common;
import com.example.abdo.task.Common.DirectionJSONParser;
import com.example.abdo.task.Common.User;
import com.example.abdo.task.Remote.IGeoCoordinates;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, com.google.android.gms.location.LocationListener {

    private ImageView profile_image;
    private TextView profile_name,address,suggest;
    private EditText latitude, longitude;
    private Button locate;
    double current_latitude, current_longitude;
    Geocoder geocoder;
    List<Address> addresses;


    private GoogleMap mMap;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static int UPDATE_INTERVAL = 1000;
    private static int FATEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 10;

    private IGeoCoordinates mService;
    private LatLng yourLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        UserInfo();
        init();
        locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mMap.clear();
               displayLocation();
                String lat = latitude.getText().toString().trim();
                String lon = longitude.getText().toString().trim();

                validationProcess(lat,lon);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermission();
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();

            }
        }
        displayLocation();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void UserInfo()  // Set User Info (Profile Pic & User name)
    {
        profile_image = findViewById(R.id.profile_image);
        profile_name = findViewById(R.id.profile_name);
        Bundle bundle = getIntent().getExtras();
        String name = bundle.getString("name");
        String id = bundle.getString("id");
        String url = "https://graph.facebook.com/" + id + "/picture?height=200&width=200";
        profile_name.setText(name);
        Picasso.get().load(url).placeholder(R.drawable.load).into(profile_image);

    }

    private void init() // init
    {
        suggest=findViewById(R.id.suggest);
        address=findViewById(R.id.address);
        mService = Common.getGeoCodeService();
        latitude = findViewById(R.id.lat);
        longitude = findViewById(R.id.lon);
        locate = findViewById(R.id.locate);
    }

    private void validationProcess(String lat, String lon)
    {
        if (lat != null && !lat.isEmpty() && lon != null && !lon.isEmpty()) {
            boolean result=isValidLatLng(Double.parseDouble(lat),Double.parseDouble(lon));
            if(result)
            {
                getLocationInfo(lat, lon);
                drawroute(lat, lon);
            }
            else
                Toast.makeText(MapsActivity.this, "InValid Values", Toast.LENGTH_SHORT).show();
        }
        else if(lat == null | lat.isEmpty() | lon == null | lon.isEmpty())
        {
            Toast.makeText(MapsActivity.this, "Empty Field", Toast.LENGTH_SHORT).show();
        }

    }


    public boolean isValidLatLng(double lat, double lng) // validate Location's latitude and longtitude values
    {
        if(lat < -90 || lat > 90)
        {
            return false;
        }
        else if(lng < -180 || lng > 180)
        {
            return false;
        }
        return true;
    }

    private void getLocationInfo(String lat, String lon) // try to get address from Location's lat & lng
    {
        geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(Double.parseDouble(lat), Double.parseDouble(lon), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String add = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String country = addresses.get(0).getCountryName();
            String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
            if(add !=null && ! add.isEmpty())
            {
                suggest.setVisibility(View.VISIBLE);
                address.setText(add);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawroute(String lat, String lon) // draw route between Current Location & Destination
    {

        LatLng SourceLocation = new LatLng(Double.parseDouble(User.getLat()), Double.parseDouble(User.getLog()));
        LatLng DestLocation = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
        bitmap = Common.scaleBitmap(bitmap, 85, 85);
        MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .title("Destination ")
                .position(DestLocation);
        mMap.addMarker(marker);

        mService.getDirections(SourceLocation.latitude + "," + SourceLocation.longitude,
                DestLocation.latitude + "," + DestLocation.longitude).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                new ParserTask().execute(response.body().toString());
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });

    }


    private void createLocationRequest()  // createLocationRequest
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    protected synchronized void buildGoogleApiClient() // buildGoogleApiClient
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();

    }

    private boolean checkPlayServices()  //checkPlayServices
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();

            } else {
                Toast.makeText(this, "This Device Is not Support", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void requestRuntimePermission() //requestRuntimePermission
    {

        ActivityCompat.requestPermissions(this, new String[]
                {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,

                }, LOCATION_PERMISSION_REQUEST);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();

                    }
                }
                break;
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermission();
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {

                current_latitude = mLastLocation.getLatitude();
                current_longitude = mLastLocation.getLongitude();

                User.setLat(String.valueOf(current_latitude));
                User.setLog(String.valueOf(current_longitude));

                // Add Marker and Move Camera
                yourLocation = new LatLng(current_latitude, current_longitude);
                mMap.addMarker(new MarkerOptions().position(yourLocation).title("Current Location "));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(yourLocation));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

                // After Add Marker for this location , Add marker for this order and draw route


            } else {
               // Toast.makeText(this, "Could not get the Location", Toast.LENGTH_SHORT).show();
            }
        }
    } // Display Current Location

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }


    public void onStatusChanged(String provider, int status, Bundle extras) {

    }


    public void onProviderEnabled(String provider) {

    }


    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>>
        // Set points between Current & Destination
        // to draw route over it
    {

        ProgressDialog mDialog = new ProgressDialog(MapsActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.setMessage("Please Waiting ....");
            mDialog.show();

        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();

                routes = parser.parse(jObject);


            } catch (JSONException e) {
                e.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            for (int i = 0; i < lists.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = lists.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat, lng);
                    points.add(position);

                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);

            }
            try {
                mMap.addPolyline(lineOptions);
            } catch (Throwable e) {
                Toast.makeText(MapsActivity.this, "Could not Find Route to Display", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }




        }
    }
}
