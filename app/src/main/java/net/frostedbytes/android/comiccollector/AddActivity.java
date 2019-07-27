package net.frostedbytes.android.comiccollector;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.util.HashMap;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.common.RetrieveComicSeriesDataTask;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesFragment;
import net.frostedbytes.android.comiccollector.fragments.ManualSearchFragment;
import net.frostedbytes.android.comiccollector.fragments.SystemMessageFragment;
import net.frostedbytes.android.comiccollector.fragments.TutorialFragment;
import net.frostedbytes.android.comiccollector.fragments.UserPreferenceFragment;
import net.frostedbytes.android.comiccollector.models.ComicBook;
import net.frostedbytes.android.comiccollector.models.ComicPublisher;
import net.frostedbytes.android.comiccollector.models.ComicSeries;
import net.frostedbytes.android.comiccollector.models.User;

public class AddActivity extends BaseActivity implements
  ComicSeriesFragment.OnComicSeriesListener,
  ManualSearchFragment.OnManualSearchListener,
  TutorialFragment.OnTutorialListener {

  private static final String TAG = BASE_TAG + "AddActivity";

  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

  private HashMap<String, ComicSeries> mComicSeries;
  private ComicBook mCurrentComicBook;
  private File mCurrentImageFile;
  private Bitmap mImageBitmap;
  private HashMap<String, ComicPublisher> mPublishers;
  private int mRotationAttempts;
  private User mUser;

  /*
      AppCompatActivity Override(s)
   */
  @Override
  public void onBackPressed() {

    LogUtils.debug(TAG, "++onBackPressed()");
    if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
      setResultAndFinish(RESULT_CANCELED, null, null, "");
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LogUtils.debug(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_add);

    Toolbar mAddToolbar = findViewById(R.id.add_toolbar);
    setSupportActionBar(mAddToolbar);
    mProgressBar = findViewById(R.id.add_progress);

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.add_fragment_container);
      if (fragment != null) {
        updateTitle(fragment);
      }
    });

    if (getIntent().hasExtra(BaseActivity.ARG_USER)) {
      mUser = (User) getIntent().getSerializableExtra(BaseActivity.ARG_USER);
    }

    if (getIntent().hasExtra(BaseActivity.ARG_COMIC_PUBLISHERS)) {
      mPublishers = (HashMap<String, ComicPublisher>)getIntent().getSerializableExtra(BaseActivity.ARG_COMIC_PUBLISHERS);
    }

    if (getIntent().hasExtra(BaseActivity.ARG_COMIC_SERIES)) {
      mComicSeries = (HashMap<String, ComicSeries>)getIntent().getSerializableExtra(BaseActivity.ARG_COMIC_SERIES);
    }

    if (User.isValid(mUser)) {
      if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
        checkForCameraPermission();
      } else {
        setResultAndFinish(BaseActivity.RESULT_ADD_FAILED, null, null, getString(R.string.err_no_camera_detected));
      }
    } else {
      setResultAndFinish(
        BaseActivity.RESULT_ADD_FAILED,
        null,
        null,
        getString(R.string.err_unknown_user));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.menu_add, menu);
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    mComicSeries = null;
    mCurrentImageFile = null;
    mImageBitmap = null;
    mPublishers = null;
    mUser = null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    LogUtils.debug(TAG, "++onOptionsItemSelected(%s)", item.getTitle());
    if (item.getItemId() == R.id.action_cancel) {
      setResultAndFinish(RESULT_CANCELED, null, null, "");
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
        setResultAndFinish(RESULT_CANCELED,null, null, "");
      } else {
        if (BuildConfig.DEBUG) { // use static file in storage
          File f = new File(getString(R.string.debug_path), getString(R.string.debug_file_name));
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
            setResultAndFinish(BaseActivity.RESULT_ADD_FAILED, null, null, getString(R.string.err_image_empty));
          }
        } else {
          setResultAndFinish(BaseActivity.RESULT_ADD_FAILED, null, null, getString(R.string.err_image_not_found));
        }
      }
    } else {
      setResultAndFinish(BaseActivity.RESULT_ADD_FAILED, null, null, getString(R.string.unknown_request_code));
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
  public void onComicSeriesActionComplete(ComicSeries comicSeries) {

    if (comicSeries != null && comicSeries.isValid()) {
      LogUtils.debug(TAG, "++onComicSeriesActionComplete(%s)", comicSeries.toString());
      comicSeries.AddedDate = Calendar.getInstance().getTimeInMillis();
      comicSeries.IsFlagged = true;

      String queryPath = PathUtils.combine(ComicSeries.ROOT, comicSeries.getProductId());
      FirebaseFirestore.getInstance().document(queryPath).set(comicSeries, SetOptions.merge()).addOnCompleteListener(task -> {

        if (task.isSuccessful()) {
          setResultAndFinish(BaseActivity.RESULT_ADD_SUCCESS, comicSeries, mCurrentComicBook, "");
        } else {
          setResultAndFinish(
            BaseActivity.RESULT_ADD_FAILED,
            null,
            null,
            getString(R.string.err_add_comic_series));
        }
      });
    } else {
      LogUtils.debug(TAG, "++onComicSeriesActionComplete(null)");
      setResultAndFinish(
        BaseActivity.RESULT_ADD_FAILED,
        null,
        null,
        getString(R.string.err_add_comic_series));
    }
  }

  @Override
  public void onManualSearchActionComplete(ComicBook comicBook) {

    LogUtils.debug(TAG, "++onManualSearchActionComplete(%s)", comicBook.toString());
    if (comicBook.isValid()) {
      mProgressBar.setIndeterminate(true);
      queryInUserComicBooks(comicBook);
    } else {
      mProgressBar.setIndeterminate(false);
      LogUtils.warn(TAG, getString(R.string.err_manual_search));
      mSnackbar = Snackbar.make(
        findViewById(R.id.add_fragment_container),
        getString(R.string.err_manual_search),
        Snackbar.LENGTH_INDEFINITE);
      mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
      mSnackbar.show();
    }
  }

  @Override
  public void onManualSearchCancel() {

    LogUtils.debug(TAG, "++onManualSearchCancel()");
    LogUtils.debug(TAG, getString(R.string.add_comic_book_canceled));
    setResultAndFinish(RESULT_CANCELED,null, null, "");
  }

  @Override
  public void onTutorialContinue() {

    LogUtils.debug(TAG, "++onTutorialContinue()");
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
    ComicPublisher comicPublisher = mPublishers.get(comicSeries.PublisherId);
    if (comicPublisher != null) {
      replaceFragment(ComicSeriesFragment.newInstance(comicSeries, comicPublisher));
    } else {
      setResultAndFinish(
        BaseActivity.RESULT_ADD_FAILED,
        null,
        null,
        getString(R.string.err_unknown_publisher));
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

  private void getIssueIdFromUser(ComicBook comic) {

    LogUtils.debug(TAG, "++getIssueIdFromUser(%s)", comic);
    if (mSnackbar != null && mSnackbar.isShown()) {
      mSnackbar.dismiss();
    }

    replaceFragment(ManualSearchFragment.newInstance(comic));
  }

  private void queryInUserComicBooks(ComicBook comicBook) {

    LogUtils.debug(TAG, "++queryInUserComicBooks(%s)", comicBook.toString());
    // comicBook should have (at a minimum)
    //   - PublisherId
    //   - SeriesId
    //   - IssueNumber, CoverVariant, & PrintRun
    ComicBook foundBook = null;
    ComicSeries series = mComicSeries.get(comicBook.getProductId());
    if (series != null) {
      for (ComicBook book : series.ComicBooks) {
        if (book.getFullId().equals(comicBook.getFullId())) {
          LogUtils.debug(TAG, "Found %s", book.toString());
          foundBook = book;
          break;
        }
      }
    }

    mProgressBar.setIndeterminate(false);
    if (foundBook != null) { // book found, show for edit
      setResultAndFinish(RESULT_OK, null, foundBook, "");
    } else { // look up series info
      ComicPublisher publisher = mPublishers.get(comicBook.PublisherId);
      if (publisher != null) {
        if (series == null) { // search for more info via REST API before asking user for information
          mCurrentComicBook = comicBook;
          series = new ComicSeries();
          series.PublisherId = comicBook.PublisherId;
          series.Id = comicBook.SeriesId;
          new RetrieveComicSeriesDataTask(this, series).execute();
        } else { // new book and known series, proceed to add
          setResultAndFinish(BaseActivity.RESULT_ADD_SUCCESS, series, comicBook, "");
        }
      } else {
        setResultAndFinish(
          BaseActivity.RESULT_ADD_FAILED,
          null,
          null,
          getString(R.string.err_unknown_publisher));
        LogUtils.error(TAG, String.format(Locale.US, "Unknown Product Code: %s", comicBook.getFullId()));
      }
    }
  }

  private void replaceFragment(Fragment fragment) {

    LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.add_fragment_container, fragment);
    if (fragment.getClass().getName().equals(ManualSearchFragment.class.getName())) {
      fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    fragmentTransaction.addToBackStack(fragment.getClass().getName());
    LogUtils.debug(TAG, "Back stack count: %d", fragmentManager.getBackStackEntryCount());
    fragmentTransaction.commitAllowingStateLoss();
  }

  private void scanImageForProductCode() {

    LogUtils.debug(TAG, "++scanImageForProductCode()");
    if (mImageBitmap != null) {
      mProgressBar.setIndeterminate(true);
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
          .setNegativeButton(R.string.cancel, (dialog, id) -> {
            mProgressBar.setIndeterminate(false);
            dialog.cancel();
          });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
      } else {
        useFirebaseBarcodeScanning();
      }
    } else {
      setResultAndFinish(BaseActivity.RESULT_ADD_FAILED, null, null, getString(R.string.err_image_not_loaded));
    }
  }

  private void setResultAndFinish(int resultCode, ComicSeries comicSeries, ComicBook comicBook, String message) {

    LogUtils.debug(TAG, "++setResultAndFinish(%d, ComicSeries, ComicBook, %s)", resultCode, message);
    Intent resultIntent = new Intent();
    resultIntent.putExtra(BaseActivity.ARG_COMIC_SERIES, comicSeries);
    resultIntent.putExtra(BaseActivity.ARG_COMIC_BOOK, comicBook);
    resultIntent.putExtra(BaseActivity.ARG_MESSAGE, message);
    setResult(resultCode, resultIntent);
    finish();
  }

  private void showPictureIntent() {

    LogUtils.debug(TAG, "++showPictureIntent()");
    replaceFragment(SystemMessageFragment.newInstance(""));
    deleteImageFile();
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      try {
        mCurrentImageFile = createImageFile();
      } catch (IOException e) {
        Crashlytics.logException(e);
      }

      if (mCurrentImageFile != null) {
        Uri photoURI = FileProvider.getUriForFile(
          this,
          "net.frostedbytes.android.comiccollector.fileprovider",
          mCurrentImageFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, BaseActivity.REQUEST_IMAGE_CAPTURE);
      } else {
        setResultAndFinish(BaseActivity.RESULT_ADD_FAILED, null, null, getString(R.string.err_photo_file_not_found));
      }
    } else {
      setResultAndFinish(BaseActivity.RESULT_ADD_FAILED,null, null, getString(R.string.err_camera_intent_failed));
    }
  }

  private void takePictureIntent() {

    LogUtils.debug(TAG, "++takePictureIntent()");
    if (mSnackbar != null && mSnackbar.isShown()) {
      mSnackbar.dismiss();
    }

    if (mUser.ShowBarcodeHint) {
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
    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
      .getVisionBarcodeDetector(options);
    com.google.android.gms.tasks.Task<java.util.List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
      .addOnCompleteListener(task -> {

        if (task.isSuccessful() && task.getResult() != null) {
          ComicBook comic = new ComicBook();
          for (FirebaseVisionBarcode barcode : task.getResult()) {
            if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_PRODUCT) {
              String barcodeValue = barcode.getDisplayValue();
              LogUtils.debug(TAG, "Found a bar code: %s", barcodeValue);
              if (barcodeValue != null && !barcodeValue.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID)) {
                comic.parseProductCode(barcodeValue);
              }
            } else {
              LogUtils.warn(
                TAG,
                "Unexpected bar code: %s (%d)",
                barcode.getDisplayValue(),
                barcode.getValueType());
            }
          }

          ComicPublisher publisher = mPublishers.get(comic.PublisherId);
          if (publisher == null) {
            LogUtils.warn(TAG, "PublisherId is unknown: %s", comic.PublisherId);
            setResultAndFinish(
              BaseActivity.RESULT_ADD_FAILED,
              null,
              null,
              getString(R.string.err_unknown_publisher));
          } else {
            LogUtils.debug(TAG, "Publisher: %s (%s)", publisher.Name, comic.PublisherId);
            if (!comic.SeriesId.isEmpty() && !comic.SeriesId.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID)) {
              getIssueIdFromUser(comic);
            } else if (mRotationAttempts < 3) {
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
              replaceFragment(ManualSearchFragment.newInstance(new ComicBook()));
            }
          }
        } else {
          setResultAndFinish(
            BaseActivity.RESULT_ADD_FAILED,
            null,
            null,
            getString(R.string.err_bar_code_task));
        }
      });
  }
}
