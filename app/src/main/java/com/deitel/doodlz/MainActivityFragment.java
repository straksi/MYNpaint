package com.deitel.doodlz;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;

import yuku.ambilwarna.AmbilWarnaDialog;

import static android.app.Activity.RESULT_CANCELED;

public class MainActivityFragment extends Fragment {
   private static final String TAG = "1";
   private DoodleView doodleView; // handles touch events and draws
   private float acceleration;
   private float currentAcceleration;
   private float lastAcceleration;
   private boolean dialogOnScreen = false;
   File directory;
   Uri orgUri;
   String path;
   Matrix matrix;
   int color = 0xff000000;

   // value used to determine whether user shook the device to erase
   private static final int ACCELERATION_THRESHOLD = 100000;
   // used to identify the request for using external storage, which
   // the save image feature needs
   private static final int SAVE_IMAGE_PERMISSION_REQUEST_CODE = 1;
   private static final int READ_IMAGE_PERMISSION_REQUEST_CODE = 2;
   private static final int OPEN_CAMERA_PERMISSION_REQUEST_CODE = 3;

   // called when Fragment's view needs to be created
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);
      View view = inflater.inflate(R.layout.fragment_main, container, false);

      setHasOptionsMenu(true); // this fragment has menu items to display
      // get reference to the DoodleView
      doodleView = (DoodleView) view.findViewById(R.id.doodleView);
      // initialize acceleration values
      acceleration = 0.00f;
      currentAcceleration = SensorManager.GRAVITY_EARTH;
      lastAcceleration = SensorManager.GRAVITY_EARTH;
      return view;
   }

   void openColorDialog(boolean supportsAlpha) {
      AmbilWarnaDialog dialog = new AmbilWarnaDialog(getActivity(), color, supportsAlpha, new AmbilWarnaDialog.OnAmbilWarnaListener() {
         @Override
         public void onOk(AmbilWarnaDialog dialog, int color) {
            MainActivityFragment.this.color = color;
            Log.d("color", String.format("Current color: 0x%08x", color));
            setColor();
         }

         @Override
         public void onCancel(AmbilWarnaDialog dialog) {
            Log.d("button","cancel");
         }
      });
      dialog.show();
   }


   // start listening for sensor events
   @Override
   public void onResume() {
      super.onResume();
      enableAccelerometerListening(); // listen for shake event
   }

   // enable listening for accelerometer events
   private void enableAccelerometerListening() {
      // get the SensorManager
      SensorManager sensorManager =
              (SensorManager) getActivity().getSystemService(
                      Context.SENSOR_SERVICE);

      // register to listen for accelerometer events
      sensorManager.registerListener(sensorEventListener,
              sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
              SensorManager.SENSOR_DELAY_NORMAL);
   }

   // stop listening for accelerometer events
   @Override
   public void onPause() {
      super.onPause();
      disableAccelerometerListening(); // stop listening for shake
   }

   // disable listening for accelerometer events
   private void disableAccelerometerListening() {
      // get the SensorManager
      SensorManager sensorManager =
              (SensorManager) getActivity().getSystemService(
                      Context.SENSOR_SERVICE);

      // stop listening for accelerometer events
      sensorManager.unregisterListener(sensorEventListener,
              sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
   }

   // event handler for accelerometer events
   private final SensorEventListener sensorEventListener =
           new SensorEventListener() {
              // use accelerometer to determine whether user shook device
              @Override
              public void onSensorChanged(SensorEvent event) {
                 // ensure that other dialogs are not displayed
                 if (!dialogOnScreen) {
                    // get x, y, and z values for the SensorEvent
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    // save previous acceleration value
                    lastAcceleration = currentAcceleration;

                    // calculate the current acceleration
                    currentAcceleration = x * x + y * y + z * z;

                    // calculate the change in acceleration
                    acceleration = currentAcceleration *
                            (currentAcceleration - lastAcceleration);

                    // if the acceleration is above a certain threshold
                    if (acceleration > ACCELERATION_THRESHOLD)
                       confirmErase();
                 }
              }

              // required method of interface SensorEventListener
              @Override
              public void onAccuracyChanged(Sensor sensor, int accuracy) {}
           };

   // confirm whether image should be erase
   private void confirmErase() {
      EraseImageDialogFragment fragment = new EraseImageDialogFragment();
      fragment.show(getFragmentManager(), "erase dialog");
   }

   // displays the fragment's menu items

   public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      super.onCreateOptionsMenu(menu, inflater);
      inflater.inflate(R.menu.doodle_fragment_menu, menu);
   }

   // handle choice from options menu
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      // switch based on the MenuItem id
      switch (item.getItemId()) {
         case R.id.undo:
            doodleView.undoDrawing();
            return true;
         case R.id.redo:
            doodleView.redoDrawing();
            return true;
         case R.id.tool:
            ToolDialogFragment toolDialog = new ToolDialogFragment();
            toolDialog.show(getFragmentManager(), "tool dialog");
            return true;
         case R.id.color:
            openColorDialog(true);
            return true; // consume the menu event
         case R.id.line_width:
            LineWidthDialogFragment widthDialog =
                    new LineWidthDialogFragment();
            widthDialog.show(getFragmentManager(), "line width dialog");
            return true; // consume the menu event
         case R.id.delete_drawing:
            confirmErase(); // confirm before erasing image
            return true; // consume the menu event
         case R.id.save:
            saveImage(); // check permission and save current image
            return true; // consume the menu event
         case R.id.open:
            openImage(); // check permission and save current image
            return true; // consume the menu event
         case R.id.camera:
            openCamera(); // check permission and save current image
            return true; // consume the menu event
         case R.id.print:
            doodleView.printImage(); // print the current images
            return true; // consume the menu event
         case R.id.settings:
            Intent i = new Intent(getActivity(), PrefActivity.class);
            startActivity(i);
            return true;
      }

      return super.onOptionsItemSelected(item);
   }

   private void requestFilePermission(){


      // shows an explanation for why permission is needed
      if (shouldShowRequestPermissionRationale(
              Manifest.permission.READ_EXTERNAL_STORAGE)) {
         AlertDialog.Builder builder =
                 new AlertDialog.Builder(getActivity());

         // set Alert Dialog's message
         builder.setMessage(R.string.permission_explanation);

         // add an OK button to the dialog
         builder.setPositiveButton(android.R.string.ok,
                 new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       // request permission
                       requestPermissions(new String[]{
                                       Manifest.permission.READ_EXTERNAL_STORAGE},
                               READ_IMAGE_PERMISSION_REQUEST_CODE);
                    }
                 }
         );
         // display the dialog
         builder.create().show();
      }
      else {
         // request permission
         requestPermissions(
                 new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                 READ_IMAGE_PERMISSION_REQUEST_CODE);
      }
   }

   private void requestCameraPermission(){


      // shows an explanation for why permission is needed
      if (shouldShowRequestPermissionRationale(
              Manifest.permission.CAMERA)) {
         AlertDialog.Builder builder =
                 new AlertDialog.Builder(getActivity());

         // set Alert Dialog's message
         builder.setMessage(R.string.permission_explanation_c);

         // add an OK button to the dialog
         builder.setPositiveButton(android.R.string.ok,
                 new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       // request permission
                       requestPermissions(new String[]{
                                       Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                               OPEN_CAMERA_PERMISSION_REQUEST_CODE);
                    }
                 }
         );
         // display the dialog
         builder.create().show();
      }
      else {
         // request permission
         requestPermissions(
                 new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                 OPEN_CAMERA_PERMISSION_REQUEST_CODE);
      }
   }

   private void openImage() {

      if(getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
              PackageManager.PERMISSION_GRANTED) {
         // if app already has permission to read from external storage
         Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

         startActivityForResult(intent, 0);
      }
      else requestFilePermission();
   }

   private void openCamera() {
      if(getContext().checkSelfPermission(Manifest.permission.CAMERA) ==
              PackageManager.PERMISSION_GRANTED) {
         Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
         orgUri = generateFileUri();
         intent.putExtra(MediaStore.EXTRA_OUTPUT, orgUri);
         startActivityForResult(intent, 1);
      }
      else requestCameraPermission();
   }

   private void saveImage() {

      if(getContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
              PackageManager.PERMISSION_GRANTED){ // if app already has permission to write to external storage
         doodleView.saveImage(); // save the image
      }
      else requestFilePermission();

   }

   // called by the system when the user either grants or denies the
   // permission for saving an image
   @Override
   public void onRequestPermissionsResult(int requestCode,
                                          String[] permissions, int[] grantResults) {
      // switch chooses appropriate action based on which feature
      // requested permission
      switch (requestCode) {
         case SAVE_IMAGE_PERMISSION_REQUEST_CODE:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
               doodleView.saveImage(); // save the image
            break;
         case READ_IMAGE_PERMISSION_REQUEST_CODE:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
               openImage(); // open the image
            break;
         case OPEN_CAMERA_PERMISSION_REQUEST_CODE:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
               openCamera(); // open camera
            break;
      }
   }

   // returns the DoodleView
   public DoodleView getDoodleView() {
      return doodleView;
   }

   private void setColor(){
      doodleView.setDrawingColor(color);
   }

   // indicates whether a dialog is displayed
   public void setDialogOnScreen(boolean visible) {
      dialogOnScreen = visible;
   }


   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      super.onActivityResult(requestCode, resultCode, intent);

      int reqHeight = doodleView.getHeight();
      int reqWidth = doodleView.getWidth();

      Log.d("REQUESTCODE", String.valueOf(requestCode));

      switch(requestCode) {
         case 0: if(intent!=null){
            orgUri = intent.getData();
            path = getRealPathFromURI(orgUri);
            Log.d("path", getRealPathFromURI(orgUri));
         }
            break;
         case 1:
            path = orgUri.getPath();
            break;
      }
      if (resultCode == getActivity().RESULT_OK) {
         doodleView.clear();

         BitmapFactory.Options options = new BitmapFactory.Options();
         options.inJustDecodeBounds = true;

         Bitmap bitmap = BitmapFactory.decodeFile(path, options);

         Log.d("log", String.format("bitmap = %s, width = %s, height = %s, mimetype = %s",
                 bitmap, options.outWidth, options.outHeight, options.outMimeType));


         Log.d("reqHeight", String.valueOf(reqHeight));
         Log.d("reqWidth", String.valueOf(reqWidth));
         options.inSampleSize = calculateInSampleSize(options, reqWidth,
                 reqHeight);
         options.inJustDecodeBounds = false;

         bitmap = BitmapFactory.decodeFile(path, options);

         String info =
                 String.format("Info: size = %s x %s, bytes = %s (%s), config = %s",
                         bitmap.getWidth(),
                         bitmap.getHeight(),
                         bitmap.getByteCount(),
                         bitmap.getRowBytes(),
                         bitmap.getConfig());
         Log.d("Bitmap", info);

         int width= bitmap.getWidth();
         int heigth = bitmap.getHeight();
         float bWidth=(float) bitmap.getWidth();
         float bHeight =(float)bitmap.getHeight();
         float degrees;
         float mx;
         if (width>heigth) { degrees= -90;   mx=  reqWidth/bHeight;}
         else { degrees= 0; mx=  reqWidth/bWidth;}


         Log.d("Mx", String.valueOf(mx));
         Log.d("Degrees", String.valueOf(degrees));

         Log.d("Scaled_reqHeight", String.valueOf(reqHeight));
         Log.d("Scaled_reqWidth", String.valueOf(reqWidth));

         matrix = new Matrix();
         matrix.setScale(mx,mx);
         matrix.postRotate(degrees);
         if(width>heigth)  matrix.postTranslate(reqWidth,0);

         Bitmap bmp;
         bmp = Bitmap.createBitmap(bitmap,0,0,width,heigth, matrix, true);

         info =
                 String.format("New matrix: size = %s x %s, bytes = %s (%s), config = %s",
                         bmp.getWidth(),
                         bmp.getHeight(),
                         bmp.getByteCount(),
                         bmp.getRowBytes(),
                         bmp.getConfig());
         Log.d("New Bitmap", info);


         doodleView.setImageBitmap(bmp);
      }
      else if (resultCode == RESULT_CANCELED) {
         Log.d(TAG, "Canceled");
      }

   }

   private Uri generateFileUri() {
      File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/Camera"+ "/" + "photo_" + System.currentTimeMillis() + ".jpg");
      Log.d("URIURI","URI"+ Uri.fromFile(file));
      return Uri.fromFile(file);
   }

   public String getRealPathFromURI(Uri contentUri) {
      String[] proj = { MediaStore.Images.Media.DATA };
      //This method was deprecated in API level 11
      //Cursor cursor = managedQuery(contentUri, proj, null, null, null);

      CursorLoader cursorLoader = new CursorLoader(
              getContext(),
              contentUri, proj, null, null, null);
      Cursor cursor = cursorLoader.loadInBackground();

      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
   }

   public static int calculateInSampleSize(BitmapFactory.Options options,
                                           int reqWidth, int reqHeight) {
      // Реальные размеры изображения
      int height = options.outHeight;
      int width = options.outWidth;

      if (width> height) { int a=width;  width=height; height=a;}  // поворот на 90

      int inSampleSize = 1;

      if (height > reqHeight || width > reqWidth) {


         // Вычисляем наибольший inSampleSize, который будет кратным двум
         // и оставит полученные размеры больше, чем требуемые
         while ((height/ inSampleSize) > reqHeight
                 || (width/ inSampleSize) > reqWidth) {
            inSampleSize *= 2;
            Log.d("inSampleSize", String.valueOf(inSampleSize));
         }
      }
      inSampleSize/=2;
      Log.d("SampleSize", String.valueOf(inSampleSize));
      return inSampleSize;
   }
}
