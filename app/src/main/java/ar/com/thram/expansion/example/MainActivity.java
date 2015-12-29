package ar.com.thram.expansion.example;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import ar.com.thram.expansion.R;
import ar.com.thram.expansion.tools.ExpansionFile;

public class MainActivity extends AppCompatActivity {

    private ExpansionFile.Downloader expansionDownloader;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Activity activity = this;
        final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
        expansionDownloader = expansionFile.downloader(activity);
        expansionDownloader.setOnCompleteListener(new Runnable() {
            @Override
            public void run() {
                ExpansionFile.Loader loader = expansionFile.loader(activity);
                Log.e("Downloader", "Completed!");
                image = (ImageView) findViewById(R.id.image_test);
                image.setImageBitmap(loader.bitmap("after.png"));
            }
        });
        expansionDownloader.setOnProgressListener(new Runnable() {
            @Override
            public void run() {
                Log.e("Progress", "Running!");
            }
        });
    }

    @Override
    protected void onStart() {
        expansionDownloader.connect();
        super.onStart();
    }

    /**
     * Disconnect the stub from our service on stop
     */
    @Override
    protected void onStop() {
        expansionDownloader.disconnect();
        super.onStop();
    }
}
