package hackmobile.mymap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.IOException;

public class NavigateActivity extends AppCompatActivity {
    public double Xscale, Yscale, Xorigin, Yorigin,x2,y2,utmx2,utmy2;
    public Bitmap mutableBitMap;
    public Location gpslocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);
        final Uri selectedImage = Uri.parse(getIntent().getExtras().getString("imageString"));
        Xscale = getIntent().getExtras().getDouble("Xscale");
        Yscale = getIntent().getExtras().getDouble("Yscale");
        Xorigin = getIntent().getExtras().getDouble("Xorigin");
        Yorigin = getIntent().getExtras().getDouble("Yorigin");
        x2 = getIntent().getExtras().getDouble("x2");
        y2 = getIntent().getExtras().getDouble("y2");
        utmx2 = getIntent().getExtras().getDouble("utmx2");
        utmy2 = getIntent().getExtras().getDouble("utmy2");


        ImageView imageview = (ImageView) findViewById(R.id.imageView);
        final TextView myUserMessage = (TextView) findViewById(R.id.textView);
        Bitmap myPhysicalMapBitmap;
        try {
            myPhysicalMapBitmap = getBitmapFromUri(selectedImage);
            imageview.setImageBitmap(myPhysicalMapBitmap);
            mutableBitMap = myPhysicalMapBitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageview.setImageBitmap(mutableBitMap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Canvas navCanvas = new Canvas(mutableBitMap);
        final Paint red = new Paint();
        red.setColor(Color.RED);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location)
            {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                LatLon2UTM utm = new LatLon2UTM();
                utm.setVariables(latitude, longitude);

                double UTMX = utm.getEasting();
                double UTMY = utm.getNorthing(latitude);

                double x = x2 + ((UTMX-utmx2)*Xscale);
                double y = y2 + ((UTMY-utmy2)*Yscale);
                navCanvas.drawCircle((float)x,(float)y,15,red);
                myUserMessage.setText("");
                myUserMessage.append("x = "+x);
                myUserMessage.append("y = "+y);
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        if (ActivityCompat.checkSelfPermission(NavigateActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(NavigateActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET} ,10);
            }
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        gpslocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException
    {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
}
