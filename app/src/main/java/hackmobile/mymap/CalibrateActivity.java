package hackmobile.mymap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

public class CalibrateActivity extends AppCompatActivity
{
    public Location gpslocation;
    public LocationManager locationManager;
    public boolean locationUpdate = false;
    public Bitmap mutableBitMap;
    public ImageView imageview;
    public int touchCount = 0;
    public ArrayList<LocationDetails> locationDetalisList = new ArrayList<>();
    public double Xscale,Yscale,Xorigin,Yorigin;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);
        final Uri selectedImage = Uri.parse(getIntent().getExtras().getString("imageString"));
        imageview = (ImageView) findViewById(R.id.physicalMap);
        final TextView myUserMessage = (TextView) findViewById(R.id.userMessage);
        final Bitmap myPhysicalMapBitmap;
        try
        {
            myPhysicalMapBitmap = getBitmapFromUri(selectedImage);
            imageview.setImageBitmap(myPhysicalMapBitmap);
            mutableBitMap = myPhysicalMapBitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageview.setImageBitmap(mutableBitMap);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(CalibrateActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(CalibrateActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET} ,10);
            }
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        LocationListener listener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                myUserMessage.setText("");
                myUserMessage.append("GPS ready ..");
                locationUpdate = true;
                gpslocation = location;
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }
            @Override
            public void onProviderEnabled(String s) {
            }
            @Override
            public void onProviderDisabled(String s)
            {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        gpslocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        //----------
        imageview.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                Matrix matrix = new Matrix();
                imageview.getImageMatrix().invert(matrix);
                float[] pts = {event.getX(),event.getY()};
                matrix.mapPoints(pts);
                myUserMessage.setTextColor(Color.YELLOW);
                if(locationUpdate == true && touchCount < 2)
                {
                    LocationDetails l = new LocationDetails();
                    touchCount = touchCount + 1;

                    double latitude = gpslocation.getLatitude();
                    double longitude = gpslocation.getLongitude();

                    LatLon2UTM utm = new LatLon2UTM();
                    utm.setVariables(latitude, longitude);

                    Canvas navCanvas = new Canvas(mutableBitMap);
                    Paint red = new Paint();
                    red.setColor(Color.RED);
                    double x = Math.floor(pts[0]);
                    double y = Math.floor(pts[1]);

                    l.latitude = latitude;
                    l.longitude = longitude;
                    l.UTMX = utm.getEasting();
                    l.UTMY = utm.getNorthing(latitude);
                    l.mapX = x;
                    l.mapY = y;
                    locationDetalisList.add(l);

                    navCanvas.drawCircle((float)x,(float)y,30,red);
                    myUserMessage.setText("");
                    myUserMessage.append("x = " + Math.floor(pts[0]));
                    myUserMessage.append("y = " + Math.floor(pts[1]));
                    myUserMessage.append("UTM_X = "+utm.getEasting());
                    myUserMessage.append("UTM_Y = "+utm.getNorthing(latitude));
                    locationUpdate = false;
                }
                else if(touchCount >=2)
                {
                    double x1,x2,y1,y2,utmx1,utmx2,utmy1,utmy2;
                    x1 = locationDetalisList.get(0).mapX;
                    x2 = locationDetalisList.get(1).mapX;
                    y1 = locationDetalisList.get(0).mapY;
                    y2 = locationDetalisList.get(1).mapY;
                    utmx1 = locationDetalisList.get(0).UTMX;
                    utmx2 = locationDetalisList.get(1).UTMX;
                    utmy1 = locationDetalisList.get(0).UTMY;
                    utmy2 = locationDetalisList.get(1).UTMY;

                    Xscale = (x2-x1)/(utmx2-utmx1);
                    Yscale = (y2-y1)/(utmy2-utmy1);


                    Xorigin = locationDetalisList.get(0).UTMX - (locationDetalisList.get(0).mapX/Xscale);
                    Yorigin = locationDetalisList.get(0).UTMY - (locationDetalisList.get(0).mapY/Yscale);

                    Intent intent = new Intent(CalibrateActivity.this,NavigateActivity.class );
                    intent.putExtra("imageString", selectedImage.toString());
                    intent.putExtra("Xscale",Xscale);
                    intent.putExtra("Yscale",Yscale);
                    intent.putExtra("Xorigin",Xorigin);
                    intent.putExtra("Yorigin",Yorigin);
                    intent.putExtra("x2",locationDetalisList.get(1).mapX);
                    intent.putExtra("y2",locationDetalisList.get(1).mapY);
                    intent.putExtra("utmx2",locationDetalisList.get(1).UTMX);
                    intent.putExtra("utmy2",locationDetalisList.get(1).UTMY);
                    CalibrateActivity.this.startActivity(intent);

                }
                else
                {
                    myUserMessage.setText("");
                    myUserMessage.append("Waiting for GPS ....");
                }
                return false;
            }
        });
        //----------
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
