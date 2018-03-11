package com.tiei.polytech.localpointcontact;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Manifest;
import java.util.jar.Pack200;

import static android.content.Context.ALARM_SERVICE;

public class MainActivity extends AppCompatActivity {
    
	// ==================================================
	// Overlays
	// ==================================================
	
	private MapView map;

	private ItemizedOverlay<OverlayItem> itemizedOverlay;
	private ListView infoList;
	
	// ==================================================
	// Buttons
	// ==================================================
	
	protected ImageButton btnCenterMap;
	
	// ==================================================
	// Other Variables
	// ==================================================
	
	private double longitude;	//经度
	private double latitude;	//纬度
	
	LocationManager locationManager;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        setContentView(R.layout.activity_main);
		
		/* Check if the location permission is allowed by user, if not, show a permission request */
		if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
		}
		
        //generate map setting
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
		
		// Compass Overlay
		CompassOverlay myCompassOverlay;
		myCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);
		myCompassOverlay.enableCompass();
		map.getOverlays().add(myCompassOverlay);
		
		/* Location Manager */
		locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
		
		/* MyLocation Overlay */
		final MyLocationNewOverlay myLocationNewOverlay;
		myLocationNewOverlay = new MyLocationNewOverlay(map);
		myLocationNewOverlay.enableMyLocation();
		//myLocationNewOverlay.enableFollowLocation();
		map.getOverlays().add(myLocationNewOverlay);
		
		/* Scale Bar Overlay */
		ScaleBarOverlay scaleBarOverlay;
		scaleBarOverlay = new ScaleBarOverlay(map);
		map.getOverlays().add(scaleBarOverlay);
		scaleBarOverlay.setScaleBarOffset(
				(int) (getResources().getDisplayMetrics().widthPixels / 2 - getResources().getDisplayMetrics().xdpi / 2), 10);
		
		/* Location Manager Test */
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		longitude = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
		latitude  = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
		LocationListener locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				String strLongitude = "Longitude: " + location.getLongitude();
				String strLatitude = "Latitude: " + location.getLatitude();
				longitude = location.getLongitude();
				latitude = location.getLatitude();
			}
		
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		
			@Override
			public void onProviderEnabled(String provider) {}
		
			@Override
			public void onProviderDisabled(String provider) {}
		};
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,2,locationListener);
		}
		catch (SecurityException e) {}
		
		
		/* Default location and zoom level */
		IMapController mapController = map.getController();
		mapController.setZoom(17);
		if(myLocationNewOverlay.getMyLocation() != null) {
			mapController.setCenter(myLocationNewOverlay.getMyLocation());
		} else {
			mapController.setCenter(new GeoPoint(latitude,longitude));
		}
		
		/* Location Provider & Location Setting */
		/*
		FusedLocationProviderClient fusedLocationProviderClient;
			fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
			fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
				@Override
				public void onSuccess(Location location) {
					if(location != null) {
						longitude = location.getLongitude();
						latitude = location.getLatitude();
						map.getController().setCenter(new GeoPoint(latitude, longitude));
					}
				}
			});
			
		LocationRequest mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(10000);
		mLocationRequest.setFastestInterval(5000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		
		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
		SettingsClient client = LocationServices.getSettingsClient(this);
		Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
		task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
			@Override
			public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
				// All location settings are satisfied. The client can initialize
				// location requests here.
			}
		});
		task.addOnFailureListener(this, new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				int statusCode = ((ApiException) e).getStatusCode();
				switch (statusCode) {
					case CommonStatusCodes.RESOLUTION_REQUIRED:
						// Location settings are not satisfied, but this can be fixed
						// by showing the user a dialog.
						try {
							// Show the dialog by calling startResolutionForResult(),
							// and check the result in onActivityResult().
							ResolvableApiException resolvable = (ResolvableApiException) e;
							resolvable.startResolutionForResult(MainActivity.this, 0x1);
						} catch (IntentSender.SendIntentException sendEx) {
							// Ignore the error.
						}
						break;
					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
						// Location settings are not satisfied. However, we have no way
						// to fix the settings so we won't show the dialog.
						break;
				}
			}
		});
		*/
		
		/* Map Click Event */
		MapEventsReceiver mReceiver = new MapEventsReceiver() {
			@Override
			public boolean singleTapConfirmedHelper(GeoPoint p) {

				if(infoList.getVisibility() == View.GONE) {
					//infoList.setVisibility(View.VISIBLE);
				} else {
					infoList.setVisibility(View.GONE);
				}
				return false;
			}
			
			@Override
			public boolean longPressHelper(GeoPoint p) { return false; }
		};
		MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(getBaseContext(), mReceiver);
		map.getOverlays().add(mapEventsOverlay);
		
		/* Information List */

		infoList = (ListView) findViewById(R.id.list_item);
		final ArrayList<String> list = new ArrayList<>();
		list.add("Hospital");
		list.add("Tel: (+33) 07 70 00 84 80");
		list.add("Size: 100~200");
		infoList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
		infoList.setVisibility(View.GONE);

		
		/* Itemized Overlay */

		final ArrayList<OverlayItem> items = new ArrayList<>();
        OverlayItem hospital = new OverlayItem("Hospital", "Tel: (+33) 07 70 00 84 80\nSize: 100~200", new GeoPoint(43.7034,7.2663));
		items.add(hospital);
		items.add(new OverlayItem("Police Office", "Tel: (+33) 07 70 00 84 81\nSize: 10~20", new GeoPoint(43.704681, 7.265695)));
		itemizedOverlay = new ItemizedIconOverlay<>(items,
				new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
					@Override
					public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
						final ArrayList<String> newList = new ArrayList<>();
						newList.add(item.getTitle());
						newList.add(item.getSnippet());
						infoList.setAdapter(new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, newList));
						infoList.setVisibility(View.VISIBLE);
                        infoList.setBackgroundColor(Color.WHITE);
						return true; // We 'handled' this event.
					}
					
					@Override
					public boolean onItemLongPress(final int index, final OverlayItem item) {
						Toast.makeText(
								MainActivity.this,
								"Item '" + item.getTitle() + "' (index=" + index
										+ ") got long pressed", Toast.LENGTH_LONG).show();
						return false;
					}
				}, getApplicationContext());
		map.getOverlays().add(this.itemizedOverlay);

        /* Set goto Map Center Button */
		btnCenterMap = (ImageButton) findViewById(R.id.ic_center_map);
		btnCenterMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GeoPoint myPosition = myLocationNewOverlay.getMyLocation();
				map.getController().setZoom(17);
				if(myPosition == null) {
					map.getController().animateTo(new GeoPoint(latitude,longitude));
				} else {
					map.getController().animateTo(myPosition);
				}
			}
		});
		
		/* Set Alarm to Send Location periodically */
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		
    }
    
    @Override
    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }
}
