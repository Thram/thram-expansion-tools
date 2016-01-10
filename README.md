# Thram's Android expansion file tools
A simple Android Project set up to implement Expansion Files easily.
You can use this project as a Boilerplate from scratch or if you already have a project going, you can simply import the "expansion-tools" Module to your project and start using it.

## Importing the modules to your project:

After 'git clone' the repo somewhere, these are the steps to follow to implement the modules in a ongoing projetc.

### Step 1: Go to "Import Module..." on Android Studio

![step 1](http://i260.photobucket.com/albums/ii2/Thram/Development/Step%201_zpsuukrf3nm.png)

### Step 2: Look for the folder where you cloned this repo and select the "expansion-tools" folder.
![step 2](http://i260.photobucket.com/albums/ii2/Thram/Development/Step%202_zpsfsfusi8u.png)
![step 3](http://i260.photobucket.com/albums/ii2/Thram/Development/Step%203_zpskszbv5fs.png)
![step 4](http://i260.photobucket.com/albums/ii2/Thram/Development/Step%204_zpshz87oera.png)

If everything goes ok, after the Gradle sync you should see your project like this:

![step 5](http://i260.photobucket.com/albums/ii2/Thram/Development/Step%205_zpsnemzjzio.png)

### Step 3: Add the module as a dependency on your app's build.gradle.

![step 6](http://i260.photobucket.com/albums/ii2/Thram/Development/Step%206_zpseetsv7ou.png)

### And Done! You are able to use it now! :)


# API

## Class ExpansionFile:
### Constructors

* ExpansionFile(int version, int size)
* ExpansionFile(int version, int size, boolean isMain) 
* ExpansionFile(int version, int size, int patchVersion)

### Methods
* **downloader(Activity activity)**: Returns an ExpansionFile.Downloader

This is the subclass in charge of the service that download the expansion file from GPlay in case the user deleted it manually. For ex. Changing the SD Card of the phone or cleaning the Data File of the app from the settings.
This is the more complex process involved and why we have to include all those Google extra modules, and is honestly barely used, because the expansion file is downloaded with the app when you install it from GPlay, so it'll be use just when for some reason the user deleted the expansion file from the SD Card manually.
```java
public class MainActivity extends AppCompatActivity {

    private ExpansionFile.Downloader expansionDownloader;
    ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
        expansionDownloader = expansionFile.downloader(this);
        expansionDownloader.setOnCompleteListener(new Runnable() {
            @Override
            public void run() {
                Log.e("Downloader", "Completed!");
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
    
    ...
}
```
* **getResourcePath(String fileName, String prefix)**: Returns a String

It constructs the file's path based on the density of the display using the following suffixes: -ldpi, -mdpi, -hdpi, -xhdpi, -xxhdpi, -xxxhdpi.
Example:
```java
// Ex. Device density = 2
final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
expansionFile.getResourcePath("example.jpg", "drawable") // Returns "res/drawable-xhdpi/example.jpg"

```

* **getDrawableResource(String filePath)**: Returns a Bitmap

This use the getResourcePath method to grab Drawables easily from the expansion file.
```java
// Ex. Device density = 2
final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
Bitmap image = expansionFile.getDrawableResource("example.jpg") // Returns "res/drawable-xhdpi/example.jpg"

```
* **getBitmapResource(String prefix, String filePath)**: Returns a Bitmap

This use the getResourcePath method to grab a Bitmap easily from the expansion file
```java
// Ex. Device density = 2
final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
Bitmap image = expansionFile.getBitmapResource("example.jpg", "example") // Returns "res/example-xhdpi/example.jpg"

```
* **getBitmap(String filePath)**: Returns a Bitmap

Grab a Bitmap easily from the expansion file.

```java
// Ex. Device density = 2
final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
ImageView image = (ImageView) findViewById(R.id.image_test);
image.setImageBitmap(expansionFile.getBitmap("example/after.png")); // Returns "example/after.jpg" as a Bitmap

```

* **getAssetFileDescriptor(String assetPath)**: Returns an AssetFileDescriptor

Grab an AssetFileDescriptor easily from the expansion file.

```java
// Loading a video file (Remember that you need to skip the compression of every video or audio file when you create the expansion file, otherwise it won't play)
final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
player = new MediaPlayer();
AssetFileDescriptor afd = expansionFile.getAssetFileDescriptor(videoPath);
player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
player.prepareAsync();
player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
    public void onPrepared(MediaPlayer mediaPlayer) {
        player.setDisplay(holder);
        mediaPlayer.start();
    }
});

```

* **getInputStream(String filePath)**: Returns an InputStream

Grab an InputStream easily from the expansion file.

```java
final ExpansionFile expansionFile = new ExpansionFile(getResources().getInteger(R.integer.obb_version), getResources().getInteger(R.integer.obb_size));
InputStream is = expansionFile.getInputStream(filePath);
```


# This is a work in progress, but I hope this will help many Android devs and you are more than welcome to fork this project and add whatever you feel is missing, and help me to report and fix bugs.

# Thanks
# Thram
