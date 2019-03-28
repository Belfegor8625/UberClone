package com.bartoszlewandowski.uberclone.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.bartoszlewandowski.uberclone.R;
import com.bartoszlewandowski.uberclone.ViewLocationsMapActivity;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DriverRequestListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private LocationManager locationManager;
    private LocationListener locationListener;

    @BindView(R.id.requestsListView)
    ListView requestsListView;

    private ArrayList<String> nearByDriveRequests;
    private ArrayAdapter<String> adapter;

    private ArrayList<Double> passengersLatitudes;
    private ArrayList<Double> passengersLongitudes;
    private ArrayList<String> requestCarUsernames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_request_list);
        ButterKnife.bind(this);
        setUpArrayLists();
        setUpAdapter();
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            initializeLocationListener();
        }
        requestsListView.setOnItemClickListener(this);
    }

    private void setUpArrayLists() {
        nearByDriveRequests = new ArrayList<>();
        passengersLatitudes = new ArrayList<>();
        passengersLongitudes = new ArrayList<>();
        requestCarUsernames = new ArrayList<>();
    }

    private void setUpAdapter() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nearByDriveRequests);
        requestsListView.setAdapter(adapter);
        nearByDriveRequests.clear();
    }

    private void initializeLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.driverLogOutItem) {
            ParseUser.logOutInBackground(new LogOutCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        finish();
                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.btnGetRequests)
    public void onClickBtnGetRequests() {
        if (Build.VERSION.SDK_INT < 23) {
            Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateRequestsListView(currentPassengerLocation);
        } else {
            if (ContextCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(DriverRequestListActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            } else {
                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateRequestsListView(currentPassengerLocation);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                initializeLocationListener();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateRequestsListView(currentDriverLocation);
            }
        }
    }

    private void updateRequestsListView(Location driverLocation) {
        if (driverLocation != null) {
            saveDriverLocationToParse(driverLocation);
            final ParseGeoPoint driverCurrentLocation = new ParseGeoPoint(driverLocation.getLatitude(), driverLocation.getLongitude());
            ParseQuery<ParseObject> requestCarQuery = ParseQuery.getQuery("RequestCar");
            requestCarQuery.whereNear("passengerLocation", driverCurrentLocation);
            requestCarQuery.whereDoesNotExist("driverOfUser");
            requestCarQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            clearArrayLists();
                            for (ParseObject nearRequest : objects) {
                                ParseGeoPoint pLocation = (ParseGeoPoint) nearRequest.get("passengerLocation");
                                double milesDistanceToPassenger = driverCurrentLocation.distanceInMilesTo(pLocation);
                                float roundedDistanceValue = Math.round(milesDistanceToPassenger * 10f) / 10f;
                                nearByDriveRequests.add("There are " + roundedDistanceValue +
                                        " miles to " + nearRequest.get("username"));
                                passengersLatitudes.add(pLocation.getLatitude());
                                passengersLongitudes.add(pLocation.getLongitude());
                                requestCarUsernames.add(nearRequest.get("username") + "");
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        FancyToast.makeText(DriverRequestListActivity.this, "Sorry, there are no requests",
                                Toast.LENGTH_SHORT, FancyToast.CONFUSING, false).show();
                    }
                }
            });
        }
    }

    private void saveDriverLocationToParse(Location location) {
        ParseUser driver = ParseUser.getCurrentUser();
        ParseGeoPoint driverLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        driver.put("driverLocation", driverLocation);
        driver.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    FancyToast.makeText(DriverRequestListActivity.this, "Location saved",
                            Toast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                }
            }
        });
    }

    private void clearArrayLists() {
        if (nearByDriveRequests.size() > 0) {
            nearByDriveRequests.clear();
        }
        if (passengersLatitudes.size() > 0) {
            passengersLatitudes.clear();
        }
        if (passengersLongitudes.size() > 0) {
            passengersLongitudes.clear();
        }
        if (requestCarUsernames.size() > 0) {
            requestCarUsernames.clear();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Intent intent = new Intent(DriverRequestListActivity.this, ViewLocationsMapActivity.class);
            intent.putExtra("dLatitude", currentDriverLocation.getLatitude());
            intent.putExtra("dLongitude", currentDriverLocation.getLongitude());
            intent.putExtra("pLatitude", passengersLatitudes.get(position));
            intent.putExtra("pLongitude", passengersLongitudes.get(position));
            intent.putExtra("rUsername", requestCarUsernames.get(position));
            startActivity(intent);
        }
    }
}
