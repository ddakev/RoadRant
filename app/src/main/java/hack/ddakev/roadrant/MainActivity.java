package hack.ddakev.roadrant;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
//import com.ragerant.platerecognizer.PlateRecognizer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.mimecraft.Multipart;
import com.squareup.mimecraft.Part;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    Button sendButton;
    TextView plateBox;
    ImageView thumbsUp;
    ImageView thumbsDown;
    ImageView cameraIcon;
    TextView commentBox;
    String license;
    String commentS;
    int ratingN;
    double lat;
    double lon;
    final String url = "http://road-rant.com/drivers";
    ArrayList<Review> reviews;
    GoogleApiClient mGoogleApiClient;
    LocationRequest lr;
    private static final int REQUEST_FINE_LOCATION=0;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            Location lastLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLoc != null) {
                lat = lastLoc.getLatitude();
                lon = lastLoc.getLongitude();
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, lr, (com.google.android.gms.location.LocationListener) this);
        } catch(SecurityException e) {
            System.out.println(e.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.out.println("Error connecting to location services (probably, i don't really know)");
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println(location.toString());
        if(location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
        }
    }


    private class Review {
        private String licensePlate;
        private boolean rating;
        private String comment;
        private double latitude;
        private double longitude;
        private String driver_id;
        private String location_id;
        private int status; //0 - not started; 1 - sent location data; 2 - sent driver data; 3 - sent review data/finished

        public Review(String plateN, boolean rating, String comment, double lat, double longt)
        {
            this.licensePlate = plateN;
            this.rating = rating;
            this.comment = comment;
            this.latitude = lat;
            this.longitude = longt;
            this.status = 0;
            sendNext();
        }

        public String getPlate()
        {
            return licensePlate;
        }

        public boolean getRating()
        {
            return rating;
        }

        public String getComment()
        {
            return comment;
        }

        public double getLatitude()
        {
            return latitude;
        }

        public double getLongitude()
        {
            return longitude;
        }

        public void setDriverId(String id)
        {
            this.driver_id = id;
        }

        public String getDriverId()
        {
            return this.driver_id;
        }

        public void setLocationId(String id)
        {
            this.location_id = id;
        }

        public String getLocationId()
        {
            return this.getLocationId();
        }

        public void setId(String id) {
            if(this.status == 1)
                setLocationId(id);
            else if(this.status == 2)
                setDriverId(id);
        }

        public int getStatus() {
            return this.status;
        }

        public void sendNext()
        {
            if(this.status == 0)
            {
                try {
                    JSONObject jsonLocInner = new JSONObject();
                    JSONObject jsonLoc = new JSONObject();
                    jsonLocInner.put("longitude", getLongitude());
                    jsonLocInner.put("latitude", getLatitude());
                    jsonLoc.put("location", jsonLocInner.toString());
                    System.out.println(jsonLoc.toString());
                    this.status ++;
                    new HTTPAsyncSend(this, jsonLoc).execute(url);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if(this.status == 1)
            {
                try {
                    JSONObject driverInner = new JSONObject();
                    JSONObject driver = new JSONObject();
                    driverInner.put("license_plate", getPlate());
                    driver.put("driver", driverInner.toString());
                    System.out.println(driver.toString());
                    this.status ++;
                    new HTTPAsyncSend(this, driver).execute(url);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if(this.status == 2)
            {
                try {
                    JSONObject reviewInner = new JSONObject();
                    JSONObject review = new JSONObject();
                    reviewInner.put("driver_id", getDriverId());
                    reviewInner.put("location_id", getLocationId());
                    reviewInner.put("rating", getRating());
                    reviewInner.put("description", getComment());
                    review.put("review", reviewInner.toString());
                    this.status ++;
                    new HTTPAsyncSend(this, review).execute(url);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                Toast.makeText(getBaseContext(), "Review info sent!", Toast.LENGTH_LONG).show();
                //the following may or may not be necessary
                plateBox.setText("");
                commentBox.setText("");
                ratingN = -1;
                thumbsUp.setImageResource(R.drawable.thumbs_up);
                thumbsDown.setImageResource(R.drawable.thumbs_down);
            }
        }

        private class HTTPAsyncSend extends AsyncTask<String, Void, String> {
            Review sender;
            JSONObject data;

            public HTTPAsyncSend(Review sender, JSONObject json)
            {
                data = json;
                this.sender = sender;
            }

            @Override
            protected String doInBackground(String... urls)
            {

                return post(urls[0], data);
            }

            @Override
            protected void onPostExecute(String result)
            {
                //If result is good, call review to next step
                System.out.println("Result: " + result);
                try {
                    JSONObject jsonRes = new JSONObject(result);
                    String id = null;
                    if(sender.getStatus() == 1)
                        id = new JSONObject(jsonRes.get("location").toString()).getString("id");
                    else if(sender.getStatus() == 2)
                        id = new JSONObject(jsonRes.get("driver").toString()).getString("id");
                    if(id != null) {
                        sender.setId(id);
                        sender.sendNext();
                    }
                    else if(sender.getStatus() == 3)
                        System.out.println("Finished sending");
                    else
                        System.out.println("Error obtaining id, stop sending info");
                } catch (JSONException e) {
                    System.out.println("Error in parsing response JSON id");
                    e.printStackTrace();
                }
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendButton = (Button) findViewById(R.id.sendButton);
        plateBox = (TextView) findViewById(R.id.plateNumber);
        thumbsUp = (ImageView) findViewById(R.id.thumbsup);
        thumbsDown = (ImageView) findViewById(R.id.thumbsdown);
        cameraIcon = (ImageView) findViewById(R.id.cameraButton);
        commentBox = (TextView) findViewById(R.id.comment);
        ratingN = -1;
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        reviews = new ArrayList<Review>();

        lr = new LocationRequest();
        lr.setInterval(1000);
        lr.setFastestInterval(1000);
        lr.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        loadPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,REQUEST_FINE_LOCATION);

        cameraIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                dispatchTakePictureIntent();
            }
        });
        thumbsUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if(ratingN == -1)
                {
                    ((ImageView)v).setImageResource(R.drawable.thumbs_up_highlight);
                }
                else if(ratingN == 0)
                {
                    thumbsDown.setImageResource(R.drawable.thumbs_down);
                    ((ImageView)v).setImageResource(R.drawable.thumbs_up_highlight);
                }
                ratingN = 1;
            }
        });
        thumbsDown.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if(ratingN == -1)
                {
                    ((ImageView)v).setImageResource(R.drawable.thumbs_down_highlight);
                }
                else if(ratingN == 1)
                {
                    thumbsUp.setImageResource(R.drawable.thumbs_up);
                    ((ImageView)v).setImageResource(R.drawable.thumbs_down_highlight);
                }
                ratingN = 0;
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if(!isConnected()) {
                    Toast.makeText(getBaseContext(), "You're not connected to the internet", Toast.LENGTH_LONG).show();
                    return ;
                }
                license = plateBox.getText().toString();
                commentS = commentBox.getText().toString();

                if(license.equals(""))
                {
                    Toast.makeText(getBaseContext(), "Enter license plate number!", Toast.LENGTH_LONG).show();
                    return;
                }
                if(commentS.equals(""))
                {
                    Toast.makeText(getBaseContext(), "Enter comment!", Toast.LENGTH_LONG).show();
                    return;
                }
                if(ratingN == -1)
                {
                    Toast.makeText(getBaseContext(), "Choose rating!", Toast.LENGTH_LONG).show();
                    return;
                }
                //When done debugging, uncomment top line and delete from here
                Review data = new Review(license, ratingN==1, commentS, lat, lon);
                reviews.add(data);
                try {
                    JSONObject jsonLocInner = new JSONObject();
                    JSONObject jsonLoc = new JSONObject();
                    jsonLocInner.put("longitude", data.getLongitude());
                    jsonLocInner.put("latitude", data.getLatitude());
                    jsonLoc.put("location", jsonLocInner.toString());
                    System.out.println(new JSONObject(jsonLoc.get("location").toString()).getString("longitude"));

                    JSONObject driverInner = new JSONObject();
                    JSONObject driver = new JSONObject();
                    driverInner.put("license_plate", data.getPlate());
                    driver.put("driver", driverInner.toString());
                    System.out.println(driver.toString());

                    JSONObject reviewInner = new JSONObject();
                    JSONObject review = new JSONObject();
                    reviewInner.put("driver_id", "id_from_server");
                    reviewInner.put("location_id", "id_from_server");
                    reviewInner.put("rating", data.getRating());
                    reviewInner.put("description", data.getComment());
                    review.put("review", reviewInner.toString());

                    System.out.println(review.toString());

                    Toast.makeText(getBaseContext(), "Review sent! (but not really)", Toast.LENGTH_LONG).show();
                    plateBox.setText("");
                    commentBox.setText("");
                    ratingN = -1;
                    thumbsUp.setImageResource(R.drawable.thumbs_up);
                    thumbsDown.setImageResource(R.drawable.thumbs_down);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //to here
            }
        });
    }

    private void loadPermissions(String perm,int requestCode) {
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                ActivityCompat.requestPermissions(this, new String[]{perm},requestCode);
            }
        }
        else
        {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission granted");
                    mGoogleApiClient.connect();
                }
                else{
                    System.out.println("Location access not granted.");
                }
                return;
            }
        }
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public static String post(String url, JSONObject data)
    {
        /*HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
        try {
            URL sendLoc = new URL(url);
            connection = (HttpURLConnection) sendLoc.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.connect();

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(data.toString());
            out.close();

            int res = connection.getResponseCode();
            if(res == HttpURLConnection.HTTP_OK) {
                InputStreamReader in = new InputStreamReader(connection.getInputStream());
                BufferedReader bIn = new BufferedReader(in);
                String responseLine = "";
                while((responseLine = bIn.readLine()) != null)
                {
                    response.append(responseLine + "\n");
                }
                bIn.close();
                System.out.println(response.toString());
            }
            else
            {
                System.out.println(connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null)
                connection.disconnect();
        }
        return response.toString();*/
        OkHttpClient client = new OkHttpClient();
        MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
        OutputStream out = null;
        Response response = null;
        try {
            URL sendLoc = new URL(url);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(RequestBody.create("driver", MEDIA_TYPE_JSON, data.toString()))
                    .build();
            Request request = new Request.Builder()
                    .header("Content-type", "application/json")
                    .url(sendLoc)
                    .post(requestBody)
                    .build();
            response = client.newCall(request).execute();
            if (response.code() != 200) throw new IOException("Unexpected code " + response);
            else
                System.out.println(response.toString());
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response != null) {
            try {
                String res = response.body().string();
                System.out.println(res);
                return res;
            } catch(IOException e) {
                return null;
            }
        }
        else
            return null;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
            OutputStream outStream = null;
            try {

                outStream = new FileOutputStream(new File(getBaseContext().getFilesDir().getPath()+"/temp.png"));
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
                File file = new File(getBaseContext().getFilesDir().getPath()+"/temp.png");
                System.out.println(file.toString());
                System.out.println("Getting license number...");
                new HTTPAsyncSend(file).execute("https://api.openalpr.com/v1/recognize?tasks=plate&country=us&secret_key=sk_cb754bf37e493856189ecd1f");
                System.out.println("Got license number (probably)");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class HTTPAsyncSend extends AsyncTask<String, Void, String> {
        File file;

        public HTTPAsyncSend(File file)
        {
            this.file = file;
        }

        @Override
        protected String doInBackground(String... urls)
        {
            System.out.println("starting recognize...");
            String result = recognize(urls[0], this.file);
            System.out.println("after recognize: " + result);
            return result;
        }

        @Override
        protected void onPostExecute(String result)
        {
            System.out.println("Result: " + result);
            try {
                JSONObject jsonRes = new JSONObject(result);
                System.out.println(jsonRes.toString());
                JSONObject pl = jsonRes.getJSONObject("plate"); // Gets plate info
                System.out.println(pl.toString());
                JSONArray res = pl.getJSONArray("results");
                System.out.println(res.toString());
                JSONObject plate = res.getJSONObject(0); // First result
                System.out.println(plate.toString());
                String lic = plate.getString("plate");
                if(lic.length() <=3) {
                    Toast.makeText(getBaseContext(), "Sorry, didn't get that. Try again.", Toast.LENGTH_LONG).show();
                }
                else {
                    license = lic; // Plate number of first result
                    plateBox.setText(license);
                    System.out.println(license);
                }
            } catch (JSONException e) {
                Toast.makeText(getBaseContext(), "Sorry, didn't get that. Try again.", Toast.LENGTH_LONG).show();
            }
            file.delete();
        }
    }

    public static String recognize(String url, File file) {
        OkHttpClient client = new OkHttpClient();
        MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
        OutputStream out = null;
        Response response = null;
        try {
            URL sendLoc = new URL(url);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "license.png",
                            RequestBody.create(MEDIA_TYPE_PNG, file))
                    .build();
            Request request = new Request.Builder()
                    .header("Content-type", "application/json")
                    .url(sendLoc)
                    .post(requestBody)
                    .build();
            response = client.newCall(request).execute();
            if (response.code() != 200) throw new IOException("Unexpected code " + response);
            else
                System.out.println(response.toString());
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response != null) {
            try {
                String res = response.body().string();
                System.out.println(res);
                return res;
            } catch(IOException e) {
                return null;
            }
        }
        else
            return null;
    }


    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

}
