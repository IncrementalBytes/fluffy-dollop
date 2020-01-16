/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package net.whollynugatory.android.comiccollector;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.BuildConfig;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.ComicCollectorException;
import net.whollynugatory.android.comiccollector.common.LogUtils;
import net.whollynugatory.android.comiccollector.common.PathUtils;
import net.whollynugatory.android.comiccollector.common.RetrieveComicSeriesDataTask;
import net.whollynugatory.android.comiccollector.db.entity.ComicBook;
import net.whollynugatory.android.comiccollector.db.entity.ComicSeries;
import net.whollynugatory.android.comiccollector.db.views.ComicBookDetails;
import net.whollynugatory.android.comiccollector.db.views.ComicSeriesDetails;
import net.whollynugatory.android.comiccollector.fragments.Camera2Fragment;
import net.whollynugatory.android.comiccollector.fragments.CameraSourceFragment;
import net.whollynugatory.android.comiccollector.fragments.ComicSeriesFragment;
import net.whollynugatory.android.comiccollector.fragments.ManualSearchFragment;
import net.whollynugatory.android.comiccollector.fragments.TutorialFragment;
import net.whollynugatory.android.comiccollector.fragments.UserPreferenceFragment;
import net.whollynugatory.android.comiccollector.models.User;
import net.whollynugatory.android.comiccollector.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.common.BarcodeProcessor.OnBarcodeProcessorListener;
import net.whollynugatory.android.comiccollector.fragments.CameraSourceFragment.OnCameraSourceListener;
import net.whollynugatory.android.comiccollector.fragments.ComicSeriesFragment.OnComicSeriesListener;
import net.whollynugatory.android.comiccollector.fragments.ManualSearchFragment.OnManualSearchListener;
import net.whollynugatory.android.comiccollector.fragments.TutorialFragment.OnTutorialListener;

public class AddActivity extends BaseActivity implements
  OnBarcodeProcessorListener,
  OnCameraSourceListener,
  OnComicSeriesListener,
  OnManualSearchListener,
  OnTutorialListener {

  private static final String TAG = BaseActivity.BASE_TAG + "AddActivity";

  private ProgressBar mProgress;

  private CollectorViewModel mCollectorViewModel;

  private File mCurrentImageFile;
  private Bitmap mImageBitmap;
  private int mRotationAttempts;
  private User mUser;

  /*
      AppCompatActivity Override(s)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LogUtils.debug(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_add);

    mProgress = findViewById(R.id.add_progress);
    Toolbar mAddToolbar = findViewById(R.id.add_toolbar);

    setSupportActionBar(mAddToolbar);
    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.add_fragment_container);
      if (fragment != null) {
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(ComicSeriesFragment.class.getName())) {
          setTitle(getString(R.string.title_comic_series));
        } else if (fragmentClassName.equals(ManualSearchFragment.class.getName())) {
          setTitle(getString(R.string.title_gathering_data));
        } else if (fragmentClassName.equals(TutorialFragment.class.getName())) {
          setTitle(getString(R.string.title_tutorial));
        } else if (fragmentClassName.equals(CameraSourceFragment.class.getName())) {
          setTitle(getString(R.string.title_camera_source));
        }
      }
    });

    if (getIntent().hasExtra(BaseActivity.ARG_USER)) {
      mUser = (User) getIntent().getSerializableExtra(BaseActivity.ARG_USER);
    }

    if (User.isValid(mUser)) {
      if (mProgress != null) {
        mProgress.setIndeterminate(true);
      }

      mCollectorViewModel = ViewModelProviders.of(this).get(CollectorViewModel.class);
      if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
        checkForCameraPermission();
      } else {
        setFailAndFinish(R.string.err_no_camera_detected);
      }
    } else {
      setFailAndFinish(R.string.err_unknown_user);
    }
  }

  @Override
  public void onBackPressed() {

    LogUtils.debug(TAG, "++onBackPressed()");
    setCancelAndFinish();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.add, menu);
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    mCurrentImageFile = null;
    mImageBitmap = null;
    mUser = null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    LogUtils.debug(TAG, "++onOptionsItemSelected(%s)", item.getTitle());
    if (item.getItemId() == R.id.action_cancel) {
      setCancelAndFinish();
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    LogUtils.debug(TAG, "++onSaveInstanceState(Bundle)");
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    LogUtils.debug(TAG, "++onActivityResult(%d, %d, Intent)", requestCode, resultCode);
    if (requestCode == BaseActivity.REQUEST_IMAGE_CAPTURE) {
      if (resultCode != RESULT_OK) {
        LogUtils.debug(TAG, "User canceled camera intent.");
        setCancelAndFinish();
      } else {
        if (BuildConfig.DEBUG) { // use static file in storage
          File f = new File(getString(R.string.debug_path), data.getStringExtra(BaseActivity.ARG_DEBUG_FILE_NAME));
          LogUtils.debug(TAG, "Using %s", f.getAbsolutePath());
          try {
            mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
          } catch (FileNotFoundException e) {
            Crashlytics.logException(e);
          }
        } else { // capture image from camera
          if (mCurrentImageFile == null) {
            try {
              mCurrentImageFile = createImageFile();
            } catch (IOException e) {
              Crashlytics.logException(e);
            }
          }

          try {
            mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(mCurrentImageFile));
          } catch (FileNotFoundException e) {
            Crashlytics.logException(e);
          }
        }

        if (mImageBitmap != null) {
          Bitmap emptyBitmap = Bitmap.createBitmap(
            mImageBitmap.getWidth(),
            mImageBitmap.getHeight(),
            mImageBitmap.getConfig());
          if (!mImageBitmap.sameAs(emptyBitmap)) {
            scanImageForProductCode();
          } else {
            setFailAndFinish(R.string.err_image_empty);
          }
        } else {
          setFailAndFinish(R.string.err_image_not_found);
        }
      }
    } else {
      setFailAndFinish(R.string.unknown_request_code);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    LogUtils.debug(TAG, "++onRequestPermissionsResult(int, String[], int[])");
    checkForCameraPermission();
  }

  /*
    Fragment Callback(s)
   */
  @Override
  public void onBarcodeProcessed(String barcode) {

    LogUtils.debug(TAG, "onBarcodeProcessed(%s)", barcode);
    ComicBook comicBook = null;
    if (barcode != null &&
      !barcode.equals(BaseActivity.DEFAULT_PRODUCT_CODE) &&
      barcode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
      comicBook = new ComicBook();
      comicBook.parseProductCode(barcode);
    } else {
      LogUtils.warn(TAG, "Unexpected bar code: %s", barcode);
    }

    if (comicBook != null) { // we have a legit barcode
      final ComicBook workableBook = new ComicBook(comicBook);
      mCollectorViewModel.getComicSeriesByProductCode(workableBook.ProductCode).observe(this, comicSeries -> {

        if (comicSeries != null) {
          getIssueIdFromUser(workableBook);
        } else {
          new RetrieveComicSeriesDataTask(this, workableBook.ProductCode).execute();
        }
      });
    } else {
      setFailAndFinish(R.string.err_bar_code_not_found);
    }
  }

  @Override
  public void onCameraSourceRetry() {

    LogUtils.debug(TAG, "++onCameraSourceRetry()");
    replaceFragment(CameraSourceFragment.newInstance());
  }

  @Override
  public void onComicSeriesActionComplete(ComicSeriesDetails seriesDetails) {

    if (seriesDetails != null) {
      LogUtils.debug(TAG, "++onComicSeriesActionComplete(%s)", seriesDetails.toString());

      ComicSeries comicSeries = new ComicSeries();
      comicSeries.parseProductCode(seriesDetails.Id);
      comicSeries.Title = seriesDetails.Title;
      comicSeries.Volume = seriesDetails.Volume;
      comicSeries.IsFlagged = true;
      comicSeries.SubmissionDate = Calendar.getInstance().getTimeInMillis();
      comicSeries.SubmittedBy = mUser.Id;
      mCollectorViewModel.insert(comicSeries);

      // add entry for global review in firestore
      String queryPath = PathUtils.combine(ComicSeries.ROOT, comicSeries.Id);
      FirebaseFirestore.getInstance().document(queryPath).set(comicSeries, SetOptions.merge()).addOnCompleteListener(task -> {

        if (!task.isSuccessful()) { // not fatal but we need to know this information for review
          Crashlytics.logException(
            new ComicCollectorException(
              String.format(
                Locale.US,
                "%s could not write pending series: %s",
                mUser.Id,
                comicSeries.toString())));
        }

        ComicBook workableBook = new ComicBook();
        workableBook.parseProductCode(comicSeries.Id);
        getIssueIdFromUser(workableBook);
      });
    } else {
      LogUtils.debug(TAG, "++onComicSeriesActionComplete(null)");
      setFailAndFinish(R.string.err_add_comic_series);
    }
  }

  @Override
  public void onManualSearchActionComplete(ComicBook comicBook) {

    LogUtils.debug(TAG, "++onManualSearchActionComplete(%s)", comicBook.toString());
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

    // look for this specific comic book
    mCollectorViewModel.getComicBookById(comicBook.ProductCode, comicBook.IssueCode).observe(this, comicBookDetails -> {

      if (comicBookDetails != null) {
        setSuccessAndFinish(comicBookDetails);
      } else {
        mCollectorViewModel.getComicSeriesByProductCode(comicBook.ProductCode).observe(this, comicSeriesDetails -> {

          if (comicSeriesDetails != null) { // add series details to comic book
            ComicBookDetails details = new ComicBookDetails(comicBook);
            details.PublisherName = comicSeriesDetails.PublisherName;
            details.SeriesTitle = comicSeriesDetails.Title;
            details.Volume = comicSeriesDetails.Volume;
            setSuccessAndFinish(details);
          } else {
            setFailAndFinish(R.string.err_add_comic_series);
          }
        });
      }
    });
  }

  @Override
  public void onManualSearchRetry() {

    LogUtils.debug(TAG, "onManualSearchRetry()");
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

    showPictureIntent();
  }

  @Override
  public void onTutorialContinue() {

    LogUtils.debug(TAG, "++onTutorialContinue()");
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

    showPictureIntent();
  }

  @Override
  public void onTutorialShowHint(boolean show) {

    LogUtils.debug(TAG, "++onTutorialShowHint(%s)", String.valueOf(show));
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
    editor.putBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, show);
    editor.apply();
    mUser.ShowBarcodeHint = show;
  }

  /*
    Public Method(s)
   */
  public void retrieveComicSeriesComplete(ComicSeries comicSeries) {

    LogUtils.debug(TAG, "++retrieveComicSeriesComplete(%s)", comicSeries.toString());
    if (comicSeries.isValid()) {
      if (mProgress != null) {
        mProgress.setIndeterminate(false);
      }

      mCollectorViewModel.getComicPublisherById(comicSeries.PublisherId).observe(this, comicPublisher -> {

        if (comicPublisher.isValid()) {
          ComicSeriesDetails seriesDetails = new ComicSeriesDetails();
          seriesDetails.Id = comicSeries.Id;
          seriesDetails.PublisherName = comicPublisher.Name;
          seriesDetails.Title = comicSeries.Title;
          replaceFragment(ComicSeriesFragment.newInstance(seriesDetails));
        } else {
          Crashlytics.logException(
            new ComicCollectorException(
              String.format(
                Locale.US,
                "%s requested an unknown comic publisher: %s",
                mUser.Id,
                comicSeries.toString())));
        }
      });
    } else {
      setFailAndFinish(R.string.err_unknown_series);
    }
  }

  /*
    Private Method(s)
   */
  private void checkForCameraPermission() {

    LogUtils.debug(TAG, "++checkDevicePermission()");
    if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission.CAMERA)) {
        Snackbar.make(
          findViewById(R.id.add_fragment_container),
          getString(R.string.permission_camera),
          Snackbar.LENGTH_INDEFINITE)
          .setAction(
            getString(R.string.ok),
            view -> ActivityCompat.requestPermissions(
              AddActivity.this,
              new String[]{permission.CAMERA},
              BaseActivity.REQUEST_CAMERA_PERMISSIONS))
          .show();
      } else {
        ActivityCompat.requestPermissions(this, new String[]{permission.CAMERA}, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
      }
    } else {
      LogUtils.debug(TAG, "%s permission granted.", permission.CAMERA);
      takePictureIntent();
    }
  }

  private File createImageFile() throws IOException {

    LogUtils.debug(TAG, "++createImageFile()");
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    return File.createTempFile(imageFileName, ".jpg", storageDir);
  }

  private void deleteImageFile() {

    LogUtils.debug(TAG, "++deleteImageFile()");
    if (mCurrentImageFile != null && mCurrentImageFile.exists()) {
      if (mCurrentImageFile.delete()) {
        LogUtils.debug(TAG, "Removed processed image: %s", mCurrentImageFile.getName());
      } else {
        LogUtils.warn(TAG, "Unable to remove processed image: %s", mCurrentImageFile.getName());
      }
    }
  }

  private void getIssueIdFromUser(ComicBook comicBook) {

    LogUtils.debug(TAG, "++getIssueIdFromUser(%s)", comicBook.toString());
    if (mProgress != null) {
      mProgress.setIndeterminate(false);
    }

    replaceFragment(ManualSearchFragment.newInstance(comicBook));
  }

  private void replaceFragment(Fragment fragment) {

    LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
    getSupportFragmentManager()
      .beginTransaction()
      .replace(R.id.add_fragment_container, fragment)
      .addToBackStack(null)
      .commit();
  }

  private void scanImageForProductCode() {

    LogUtils.debug(TAG, "++scanImageForProductCode()");
    if (mImageBitmap != null) {
      if (mUser.IsGeek) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_debug_image, null);
        ImageView imageView = promptView.findViewById(R.id.debug_dialog_image);
        BitmapDrawable bmd = new BitmapDrawable(this.getResources(), mImageBitmap);
        imageView.setImageDrawable(bmd);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
          .setPositiveButton(R.string.ok, (dialog, id) -> useFirebaseBarcodeScanning())
          .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
      } else {
        useFirebaseBarcodeScanning();
      }
    } else {
      setFailAndFinish(R.string.err_image_not_loaded);
    }
  }

  private void setCancelAndFinish() {

    setResult(BaseActivity.RESULT_CANCELED, new Intent());
    finish();
  }

  private void setFailAndFinish(int messageId) {

    Intent resultIntent = new Intent();
    resultIntent.putExtra(BaseActivity.ARG_MESSAGE_ID, getString(messageId));
    setResult(BaseActivity.RESULT_ADD_FAILED, resultIntent);
    finish();
  }

  private void setSuccessAndFinish(ComicBookDetails comicBook) {

    Intent resultIntent = new Intent();
    resultIntent.putExtra(BaseActivity.ARG_COMIC_BOOK, comicBook);
    setResult(BaseActivity.RESULT_ADD_SUCCESS, resultIntent);
    finish();
  }

  private void showPictureIntent() {

    LogUtils.debug(TAG, "++showPictureIntent()");
    deleteImageFile();
    if (BuildConfig.DEBUG) {
      LayoutInflater layoutInflater = LayoutInflater.from(this);
      View promptView = layoutInflater.inflate(R.layout.dialog_debug_camera, null);

      Spinner spinner = promptView.findViewById(R.id.debug_spinner_file);
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
      alertDialogBuilder.setView(promptView);
      alertDialogBuilder.setCancelable(false)
        .setPositiveButton(R.string.ok, (dialog, id) -> {
          Intent debugIntent = new Intent();
          debugIntent.putExtra(BaseActivity.ARG_DEBUG_FILE_NAME, spinner.getSelectedItem().toString());
          onActivityResult(BaseActivity.REQUEST_IMAGE_CAPTURE, RESULT_OK, debugIntent);
        })
        .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

      AlertDialog alert = alertDialogBuilder.create();
      alert.show();
    } else if (mUser.UseImageCapture) {
      replaceFragment(CameraSourceFragment.newInstance());
    } else {
      replaceFragment(Camera2Fragment.newInstance());
//      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//      if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//        try {
//          mCurrentImageFile = createImageFile();
//        } catch (IOException e) {
//          Crashlytics.logException(e);
//        }
//
//        if (mCurrentImageFile != null) {
//          Uri photoURI = FileProvider.getUriForFile(
//            this,
//            "net.whollynugatory.android.comiccollector.fileprovider",
//            mCurrentImageFile);
//          takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//          startActivityForResult(takePictureIntent, BaseActivity.REQUEST_IMAGE_CAPTURE);
//        } else {
//          setFailAndFinish(R.string.err_photo_file_not_found);
//        }
//      } else {
//        setFailAndFinish(R.string.err_camera_intent_failed);
//      }
    }
  }

  private void takePictureIntent() {

    LogUtils.debug(TAG, "++takePictureIntent()");
    if (mUser.ShowBarcodeHint) {
      if (mProgress != null) {
        mProgress.setIndeterminate(false);
      }

      replaceFragment(TutorialFragment.newInstance(mUser));
    } else {
      showPictureIntent();
    }
  }

  private void useFirebaseBarcodeScanning() {

    LogUtils.debug(TAG, "++useFirebaseBarcodeScanning()");
    FirebaseVisionBarcodeDetectorOptions options =
      new FirebaseVisionBarcodeDetectorOptions.Builder()
        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_UPC_A, FirebaseVisionBarcode.FORMAT_UPC_E)
        .build();
    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

    com.google.android.gms.tasks.Task<java.util.List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
      .addOnCompleteListener(task -> {

        if (task.isSuccessful() && task.getResult() != null) {
          ComicBook comicBook = null;
          for (FirebaseVisionBarcode barcode : task.getResult()) {
            if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_PRODUCT) {
              String barcodeValue = barcode.getDisplayValue();
              LogUtils.debug(TAG, "Found a bar code: %s", barcodeValue);
              comicBook = new ComicBook();
              if (barcodeValue != null &&
                !barcodeValue.equals(BaseActivity.DEFAULT_PRODUCT_CODE) &&
                barcodeValue.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
                comicBook.parseProductCode(barcodeValue);
              } else {
                LogUtils.warn(
                  TAG,
                  "Unexpected bar code: %s (%d)",
                  barcode.getDisplayValue(),
                  barcode.getValueType());
              }
            } else {
              LogUtils.warn(
                TAG,
                "Unexpected bar code: %s (%d)",
                barcode.getDisplayValue(),
                barcode.getValueType());
            }
          }

          if (comicBook != null) { // we have a legit barcode
            final ComicBook workableBook = new ComicBook(comicBook);
            mCollectorViewModel.getComicSeriesByProductCode(workableBook.ProductCode).observe(this, comicSeries -> {

              if (comicSeries != null) {
                getIssueIdFromUser(workableBook);
              } else {
                new RetrieveComicSeriesDataTask(this, workableBook.ProductCode).execute();
              }
            });
          } else { // try rotating the image, escape after 3 attempts
            if (mRotationAttempts < 3) {
              mRotationAttempts++;
              Matrix matrix = new Matrix();
              matrix.postRotate(90);
              mImageBitmap = Bitmap.createBitmap(
                mImageBitmap,
                0,
                0,
                mImageBitmap.getWidth(),
                mImageBitmap.getHeight(),
                matrix,
                true);
              scanImageForProductCode();
            } else {
              Matrix matrix = new Matrix();
              matrix.postRotate(90);
              mImageBitmap = Bitmap.createBitmap(
                mImageBitmap,
                0,
                0,
                mImageBitmap.getWidth(),
                mImageBitmap.getHeight(),
                matrix,
                true);
              mRotationAttempts = 0;
              LogUtils.warn(TAG, "Rotated image completely and could not find a bar code.");
              replaceFragment(ManualSearchFragment.newInstance(new ComicBook()));
            }
          }
        } else {
          setFailAndFinish(R.string.err_bar_code_task);
        }
      });
  }
}
