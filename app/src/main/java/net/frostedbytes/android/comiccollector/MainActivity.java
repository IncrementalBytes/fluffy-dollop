package net.frostedbytes.android.comiccollector;

import android.Manifest;
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
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import java.util.Calendar;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.common.SortUtils;
import net.frostedbytes.android.comiccollector.common.WriteToLocalComicSeriesTask;
import net.frostedbytes.android.comiccollector.common.WriteToLocalLibraryTask;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesFragment;
import net.frostedbytes.android.comiccollector.fragments.InterludeFragment;
import net.frostedbytes.android.comiccollector.fragments.ManualSearchFragment;
import net.frostedbytes.android.comiccollector.fragments.TutorialFragment;
import net.frostedbytes.android.comiccollector.fragments.UserPreferenceFragment;
import net.frostedbytes.android.comiccollector.models.ComicBook;
import net.frostedbytes.android.comiccollector.models.ComicPublisher;
import net.frostedbytes.android.comiccollector.models.ComicSeries;
import net.frostedbytes.android.comiccollector.models.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class MainActivity extends AppCompatActivity implements
  ComicBookFragment.OnComicBookListener,
  ComicBookListFragment.OnComicBookListListener,
  ComicSeriesFragment.OnComicSeriesListener,
  ManualSearchFragment.OnManualSearchListener,
  TutorialFragment.OnTutorialListener,
  UserPreferenceFragment.OnPreferencesListener {

  private static final String TAG = BASE_TAG + MainActivity.class.getSimpleName();

  private static final int REQUEST_IMAGE_CAPTURE = 1;

  private static final int CAMERA_PERMISSIONS_REQUEST = 11;
  private static final int WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 12;

  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

  private ArrayList<ComicBook> mComicBooks;
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
      finish();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LogUtils.debug(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_main);

    Toolbar mainToolbar = findViewById(R.id.main_toolbar);
    setSupportActionBar(mainToolbar);

    mProgressBar = findViewById(R.id.main_progress);
    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
      if (fragment != null) {
        updateTitle(fragment);
      }
    });

    mComicBooks = new ArrayList<>();
    mComicSeries = new HashMap<>();
    mPublishers = new HashMap<>();
    mUser = new User();
    mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
    mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
    mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
    mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);

    mProgressBar.setIndeterminate(true);
    readServerComicPublishers();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    LogUtils.debug(TAG, "++onOptionsItemSelected(%s)", item.getTitle());
    switch (item.getItemId()) {
      case R.id.action_home:
        if (!mUser.Id.isEmpty() && !mUser.Id.equals(BaseActivity.DEFAULT_USER_ID)) {
          checkDevicePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
        } else {
          showDismissableSnackbar(getString(R.string.err_unknown_user));
        }

        break;
      case R.id.action_add:
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
          checkDevicePermission(Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST);
        } else {
          showDismissableSnackbar(getString(R.string.err_no_camera_detected));
        }

        break;
      case R.id.action_settings:
        replaceFragment(UserPreferenceFragment.newInstance(mUser));
        break;
      case R.id.action_logout:
        AlertDialog dialog = new AlertDialog.Builder(this)
          .setMessage(R.string.logout_message)
          .setPositiveButton(android.R.string.yes, (dialog1, which) -> {

            // sign out of firebase
            FirebaseAuth.getInstance().signOut();

            // sign out of google, if necessary
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
              .requestIdToken(getString(R.string.default_web_client_id))
              .requestEmail()
              .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {

              // return to sign-in activity
              startActivity(new Intent(getApplicationContext(), SignInActivity.class));
              finish();
            });
          })
          .setNegativeButton(android.R.string.no, null)
          .create();
        dialog.show();
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    LogUtils.debug(TAG, "++onActivityResult(%d, %d, Intent)", requestCode, resultCode);
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      if (BuildConfig.DEBUG) {
        File f = new File(getString(R.string.debug_path), getString(R.string.debug_file_name));
        try {
          mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
          Crashlytics.logException(e);
        }
      } else {
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
          showDismissableSnackbar(getString(R.string.err_image_empty));
        }
      } else {
        showDismissableSnackbar(getString(R.string.err_image_not_found));
      }
    } else {
      showDismissableSnackbar(getString(R.string.err_camera_data_unexpected));
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    LogUtils.debug(TAG, "++onRequestPermissionResult(int, String[], int[])");
    switch (requestCode) {
      case WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          LogUtils.debug(TAG, "WRITE_EXTERNAL_STORAGE permission granted.");
          mComicBooks = ComicBook.readLocalLibrary(getFilesDir());
          if (mComicBooks == null || mComicBooks.size() == 0) {
            readServerLibrary(); // attempt to get user's book library from cloud
          } else {
            mComicBooks.sort(new SortUtils.ByPublicationDate());
            mProgressBar.setIndeterminate(false);
            replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
          }
        } else {
          LogUtils.debug(TAG, "WRITE_EXTERNAL_STORAGE permission denied.");
          mProgressBar.setIndeterminate(false);
          showDismissableSnackbar(getString(R.string.permission_storage));
        }

        break;
      case CAMERA_PERMISSIONS_REQUEST:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          LogUtils.debug(TAG, "CAMERA_PERMISSIONS_REQUEST permission granted.");
          takePictureIntent();
        } else {
          LogUtils.debug(TAG, "CAMERA_PERMISSIONS_REQUEST permission denied.");
          mProgressBar.setIndeterminate(false);
          showDismissableSnackbar(getString(R.string.permission_camera));
        }

        break;
      default:
        LogUtils.debug(TAG, "Unknown request code: %d", requestCode);
        break;
    }
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onComicBookActionComplete(String message) {

    LogUtils.debug(TAG, "++onComicBookActionComplete(%s)", message);
    showDismissableSnackbar(message);
  }

  @Override
  public void onComicBookAddedToLibrary(ComicBook comicBook) {

    if (comicBook == null) {
      LogUtils.debug(TAG, "++onUserBookAddedToLibrary(null)");
      showDismissableSnackbar(getString(R.string.err_add_comic_book));
    } else {
      LogUtils.debug(TAG, "++onUserBookAddedToLibrary(%s)", comicBook.toString());
      mComicBooks.add(comicBook);
      new WriteToLocalLibraryTask(this, mComicBooks).execute();
    }
  }

  @Override
  public void onComicBookInit(boolean isSuccessful) {

    LogUtils.debug(TAG, "++onComicBookInit(%s)", String.valueOf(isSuccessful));
    mProgressBar.setIndeterminate(false);
  }

  @Override
  public void onComicBookRemoved(ComicBook comicBook) {

    mProgressBar.setIndeterminate(false);
    if (comicBook == null) {
      LogUtils.debug(TAG, "++onCloudyBookRemoved(null)");
      showDismissableSnackbar(getString(R.string.err_remove_comic_book));
    } else {
      LogUtils.debug(TAG, "++onCloudyBookRemoved(%s)", comicBook.toString());
      mComicBooks.remove(comicBook);
      new WriteToLocalLibraryTask(this, mComicBooks).execute();
      replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
    }
  }

  @Override
  public void onComicBookStarted() {

    LogUtils.debug(TAG, "++onComicBookStarted()");
    mProgressBar.setIndeterminate(true);
  }

  @Override
  public void onComicBookUpdated(ComicBook updatedComicBook) {

    if (updatedComicBook == null) {
      LogUtils.debug(TAG, "++onCloudyBookUpdated(null)");
      showDismissableSnackbar(getString(R.string.err_update_comic_book));
    } else {
      LogUtils.debug(TAG, "++onCloudyBookUpdated(%s)", updatedComicBook.toString());
      ArrayList<ComicBook> updatedComicBookList = new ArrayList<>();
      for (ComicBook comicBook : mComicBooks) {
        if (comicBook.getFullId().equals(updatedComicBook.getFullId())) {
          updatedComicBookList.add(updatedComicBook);
        } else {
          updatedComicBookList.add(comicBook);
        }
      }

      new WriteToLocalLibraryTask(this, updatedComicBookList).execute();
      replaceFragment(ComicBookListFragment.newInstance(updatedComicBookList, mPublishers, mComicSeries));
    }
  }

  @Override
  public void onComicListAddBook() {

    LogUtils.debug(TAG, "++onComicListAddBook()");
    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
      checkDevicePermission(Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST);
    } else {
      showDismissableSnackbar(getString(R.string.err_no_camera_detected));
    }
  }

  @Override
  public void onComicListItemSelected(ComicBook comicBook) {

    LogUtils.debug(TAG, "++onComicListItemSelected(%s)", comicBook.toString());
    replaceFragment(
      ComicBookFragment.newInstance(
        mUser.Id,
        comicBook,
        mPublishers.get(comicBook.PublisherId),
        mComicSeries.get(comicBook.getProductId())));
  }

  @Override
  public void onComicListPopulated(int size) {

    LogUtils.debug(TAG, "++onComicListPopulated(%d)", size);
    mProgressBar.setIndeterminate(false);
    if (size == 0) {
      if (mSnackbar == null || !mSnackbar.isShown()) {
        mSnackbar = Snackbar.make(
          findViewById(R.id.main_fragment_container),
          getString(R.string.err_no_data),
          Snackbar.LENGTH_LONG)
          .setAction(
            getString(R.string.add),
            view -> {
              if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                checkDevicePermission(Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST);
              } else {
                showDismissableSnackbar(getString(R.string.err_no_camera_detected));
              }
            });
        mSnackbar.show();
      }
    }
  }

  @Override
  public void onComicListSynchronize() {

    LogUtils.debug(TAG, "++onComicListSynchronize()");
    mProgressBar.setIndeterminate(true);
    readServerLibrary();
  }

  @Override
  public void onComicSeriesActionComplete(ComicSeries comicSeries) {

    if (comicSeries != null && comicSeries.isValid()) {
      LogUtils.debug(TAG, "++onComicSeriesActionComplete(%s)", comicSeries.toString());
      comicSeries.AddedDate = Calendar.getInstance().getTimeInMillis();
      comicSeries.IsFlagged = true;

      String queryPath = PathUtils.combine(ComicSeries.ROOT, comicSeries.getId());
      Trace comicSeriesTrace = FirebasePerformance.getInstance().newTrace("set_comic_series");
      comicSeriesTrace.start();
      FirebaseFirestore.getInstance().document(queryPath).set(comicSeries, SetOptions.merge()).addOnCompleteListener(task -> {

        if (task.isSuccessful()) {
          mComicSeries.put(comicSeries.getId(), comicSeries);
          replaceFragment(
            ComicBookFragment.newInstance(
              mUser.Id,
              mCurrentComicBook,
              mPublishers.get(mCurrentComicBook.PublisherId),
              comicSeries));
          comicSeriesTrace.incrementMetric("comic_series_add", 1);
          new WriteToLocalComicSeriesTask(this, new ArrayList<>(mComicSeries.values())).execute();
        } else {
          comicSeriesTrace.incrementMetric("comic_series_err", 1);
          showDismissableSnackbar(getString(R.string.err_add_comic_series));
        }

        comicSeriesTrace.stop();
      });
    } else {
      LogUtils.debug(TAG, "++onComicSeriesActionComplete(null)");
      replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
      showDismissableSnackbar(getString(R.string.err_add_comic_series));
    }
  }

  @Override
  public void onManualSearchActionComplete(ComicBook comicBook) {

    LogUtils.debug(TAG, "++onManualSearchActionComplete(%s)", comicBook.toString());
    if (comicBook.isValid()) {
      mProgressBar.setIndeterminate(true);
      queryInUserComicBooks(comicBook);
    } else {
      showDismissableSnackbar(getString(R.string.err_manual_search));
    }
  }

  @Override
  public void onManualSearchCancel() {

    LogUtils.debug(TAG, "++onManualSearchCancel()");
    replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
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

  @Override
  public void onPreferenceChanged() {

    LogUtils.debug(TAG, "++onPreferenceChanged()");
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    if (preferences.contains(UserPreferenceFragment.IS_GEEK_PREFERENCE)) {
      mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
    }

    if (preferences.contains(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE)) {
      mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, false);
    }
  }

  /*
      Public Method(s)
   */
  public void writeComicSeriesComplete(ArrayList<ComicSeries> comicSeries) {

    LogUtils.debug(TAG, "++writeComicSeriesComplete(%d)", comicSeries.size());
    mComicSeries = new HashMap<>();
    for (ComicSeries series : comicSeries) {
      if (!mComicSeries.containsKey(series.getId())) {
        mComicSeries.put(series.getId(), series);
      }
    }
  }

  public void writeLibraryComplete(ArrayList<ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++writeLibraryComplete(%d)", comicBooks.size());
    mProgressBar.setIndeterminate(false);
    mComicBooks = comicBooks;
    replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
  }

  /*
      Private Method(s)
   */
  private void addNewComicSeries(ComicSeries comicSeries) {

    LogUtils.debug(TAG, "++addNewComicSeries(%s)", comicSeries.toString());
    replaceFragment(ComicSeriesFragment.newInstance(comicSeries, mPublishers.get(comicSeries.PublisherId)));
  }

  private void checkDevicePermission(String permission, int permissionCode) {

    LogUtils.debug(TAG, "++checkDevicePermission(%s, %d)", permission, permissionCode);
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
        Snackbar.make(
          findViewById(R.id.main_fragment_container),
          getString(R.string.permission_denied_explanation),
          Snackbar.LENGTH_INDEFINITE)
          .setAction(
            getString(R.string.ok),
            view -> ActivityCompat.requestPermissions(
              MainActivity.this,
              new String[]{permission},
              permissionCode))
          .show();
      } else {
        ActivityCompat.requestPermissions(this, new String[]{permission}, permissionCode);
      }
    } else {
      switch (permissionCode) {
        case WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST:
          LogUtils.debug(TAG, "%s permission granted.", permission);
          mComicBooks = ComicBook.readLocalLibrary(getFilesDir());
          if (mComicBooks == null || mComicBooks.size() == 0) {
            readServerLibrary(); // attempt to get user's book library from cloud
          } else {
            mComicBooks.sort(new SortUtils.ByBookName());
            mProgressBar.setIndeterminate(false);
            replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
          }

          break;
        case CAMERA_PERMISSIONS_REQUEST:
          LogUtils.debug(TAG, "%s permission granted.", permission);
          takePictureIntent();
          break;
      }
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
    if (mComicBooks != null) {
      for (ComicBook comic : mComicBooks) {
        if (!comicBook.getFullId().isEmpty() && comic.getFullId().equals(comicBook.getFullId())) {
          foundBook = comic;
          break;
        }
      }
    }

    mProgressBar.setIndeterminate(false);
    if (foundBook != null) { // book found, show for edit
      replaceFragment(
        ComicBookFragment.newInstance(
          mUser.Id,
          foundBook,
          mPublishers.get(foundBook.PublisherId),
          mComicSeries.get(foundBook.getProductId())));
    } else { // look up series info
      ComicPublisher publisher = mPublishers.get(comicBook.PublisherId);
      if (publisher != null) {
        ComicSeries series = mComicSeries.get(comicBook.getProductId());
        if (series == null) {
          // save comic book for later
          mCurrentComicBook = new ComicBook(comicBook);
          series = new ComicSeries();
          series.PublisherId = comicBook.PublisherId;
          series.Id = comicBook.SeriesId;
          addNewComicSeries(series);
        } else { // new book and known series, proceed to add
          replaceFragment(
            ComicBookFragment.newInstance(
              mUser.Id,
              comicBook,
              mPublishers.get(comicBook.PublisherId),
              mComicSeries.get(comicBook.getProductId())));
        }
      } else {
        replaceFragment(ManualSearchFragment.newInstance(new ComicBook()));
        showDismissableSnackbar(getString(R.string.err_unknown_publisher));
      }
    }
  }

  private void readServerComicPublishers() {

    LogUtils.debug(TAG, "++readServerComicSeries()");
    mPublishers = new HashMap<>();
    Trace comicPublisherTrace = FirebasePerformance.getInstance().newTrace("get_comic_publishers");
    comicPublisherTrace.start();
    FirebaseFirestore.getInstance().collection(ComicPublisher.ROOT).get().addOnCompleteListener(task -> {

      if (task.isSuccessful() && task.getResult() != null) {
        for (DocumentSnapshot snapshot : task.getResult()) {
          ComicPublisher publisher = snapshot.toObject(ComicPublisher.class);
          if (publisher != null && !publisher.IsFlagged) {
            publisher.Id = snapshot.getId();
            mPublishers.put(publisher.Id, publisher);
            comicPublisherTrace.incrementMetric("comic_publisher_read", 1);
          } else {
            comicPublisherTrace.incrementMetric("comic_publisher_unread", 1);
          }
        }
      } else {
        comicPublisherTrace.incrementMetric("comic_publisher_err", 1);
      }

      comicPublisherTrace.stop();
      if (mPublishers.size() > 0) {
        mComicSeries = ComicSeries.readLocalComicSeries(getFilesDir());
        if (mComicSeries.size() == 0) {
          readServerComicSeries();
        } else {
          if (!mUser.Id.isEmpty() && !mUser.Id.equals(BaseActivity.DEFAULT_USER_ID)) {
            checkDevicePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
          } else {
            showDismissableSnackbar(getString(R.string.err_unknown_user));
          }
        }
      } else {
        showDismissableSnackbar(getString(R.string.err_setup_publisher_data));
      }
    });
  }

  private void readServerComicSeries() {

    LogUtils.debug(TAG, "++readServerComicSeries()");
    mComicSeries = new HashMap<>();
    Trace comicSeriesTrace = FirebasePerformance.getInstance().newTrace("get_comic_series");
    comicSeriesTrace.start();
    FirebaseFirestore.getInstance().collection(ComicSeries.ROOT).get().addOnCompleteListener(task -> {

      if (task.isSuccessful() && task.getResult() != null) {
        for (DocumentSnapshot snapshot : task.getResult()) {
          ComicSeries series = snapshot.toObject(ComicSeries.class);
          if (series != null) {
            String seriesId = snapshot.getId();
            if (seriesId.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
              try {
                series.PublisherId = seriesId.substring(0, BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
                series.Id = seriesId.substring(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
              } catch (Exception e) {
                series.PublisherId = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
                series.Id = BaseActivity.DEFAULT_COMIC_SERIES_ID;
              }

              if (!series.PublisherId.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID) ||
                !series.Id.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID)) {
                mComicSeries.put(seriesId, series);
                comicSeriesTrace.incrementMetric("comic_series_read", 1);
              } else {
                comicSeriesTrace.incrementMetric("comic_series_bad_format", 1);
              }
            } else {
              comicSeriesTrace.incrementMetric("comic_series_bad_format", 1);
            }
          } else {
            comicSeriesTrace.incrementMetric("comic_series_unread", 1);
          }
        }

        // update our local cache with data from cloud
        new WriteToLocalComicSeriesTask(this, new ArrayList<>(mComicSeries.values())).execute();
      } else {
        LogUtils.warn(TAG, "Reverting to asset data for ComicSeries.");
        comicSeriesTrace.incrementMetric("comic_series_err", 1);
        mComicSeries = ComicSeries.parseComicSeriesAssetFile(getAssets());
      }

      comicSeriesTrace.stop();

      // series data is loaded from either remote or last resort, time for comic library data
      if (!mUser.Id.isEmpty() && !mUser.Id.equals(BaseActivity.DEFAULT_USER_ID)) {
        checkDevicePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
      } else {
        showDismissableSnackbar(getString(R.string.err_unknown_user));
      }
    });
  }

  private void readServerLibrary() {

    LogUtils.debug(TAG, "++readServerLibrary()");
    String queryPath = PathUtils.combine(User.ROOT, mUser.Id, ComicBook.ROOT);
    LogUtils.debug(TAG, "QueryPath: %s", queryPath);
    mComicBooks = new ArrayList<>();
    Trace comicBookTrace = FirebasePerformance.getInstance().newTrace("get_user_comics");
    comicBookTrace.start();
    FirebaseFirestore.getInstance().collection(queryPath).get().addOnCompleteListener(this, task -> {

      if (task.isSuccessful() && task.getResult() != null) {
        for (DocumentSnapshot document : task.getResult().getDocuments()) {
          ComicBook comicBook = document.toObject(ComicBook.class);
          if (comicBook != null) {
            String[] segments = document.getId().split("-");
            comicBook.parseProductCode(segments[0]);
            comicBook.parseIssueCode(segments[1]);
            mComicBooks.add(comicBook);
            comicBookTrace.incrementMetric("comic_book_read", 1);
          } else {
            LogUtils.warn(TAG, "Unable to convert user book: %s", queryPath);
            comicBookTrace.incrementMetric("comic_book_unread", 1);
          }
        }

        mComicBooks.sort(new SortUtils.ByBookName());
        new WriteToLocalLibraryTask(this, mComicBooks).execute();
      } else {
        LogUtils.debug(TAG, "Could not get user book list: %s", queryPath);
        comicBookTrace.incrementMetric("comic_book_err", 1);
        onComicListPopulated(0);
      }

      comicBookTrace.stop();
    });
  }

  private void replaceFragment(Fragment fragment) {

    LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.main_fragment_container, fragment);
    if (fragment.getClass().getName().equals(ComicBookListFragment.class.getName())) {
      fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    fragmentTransaction.addToBackStack(fragment.getClass().getName());
    LogUtils.debug(TAG, "Back stack count: %d", fragmentManager.getBackStackEntryCount());
    fragmentTransaction.commit();
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
      showDismissableSnackbar(getString(R.string.err_image_not_loaded));
    }
  }

  private void showDismissableSnackbar(String message) {

    mProgressBar.setIndeterminate(false);
    LogUtils.warn(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.main_fragment_container),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }

  private void showPictureIntent() {

    LogUtils.debug(TAG, "++showPictureIntent()");
    replaceFragment(InterludeFragment.newInstance());
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
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      } else {
        showDismissableSnackbar(getString(R.string.err_photo_file_not_found));
      }
    } else {
      showDismissableSnackbar(getString(R.string.err_camera_intent_failed));
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

  private void updateTitle(Fragment fragment) {

    LogUtils.debug(TAG, "++updateTitle(%s)", fragment.getClass().getName());
    String fragmentClassName = fragment.getClass().getName();
    if (fragmentClassName.equals(ComicBookListFragment.class.getName())) {
      setTitle(getString(R.string.title_comic_library));
    } else if (fragmentClassName.equals(ComicBookFragment.class.getName())) {
      setTitle(getString(R.string.title_comic_book));
    } else if (fragmentClassName.equals(UserPreferenceFragment.class.getName())) {
      setTitle(getString(R.string.title_preferences));
    } else if (fragmentClassName.equals(TutorialFragment.class.getName())) {
      setTitle(getString(R.string.title_tutorial));
    } else if (fragmentClassName.equals(ComicSeriesFragment.class.getName())) {
      setTitle(getString(R.string.title_comic_series));
    } else if (fragmentClassName.equals(InterludeFragment.class.getName())) {
      setTitle(getString(R.string.title_please_wait));
    } else if (fragmentClassName.equals(ManualSearchFragment.class.getName())) {
      setTitle(getString(R.string.title_gathering_data));
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

          if ((!comic.SeriesId.isEmpty() && !comic.SeriesId.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID)) &&
            (!comic.PublisherId.isEmpty() && !comic.PublisherId.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID))) {
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
            replaceFragment(ManualSearchFragment.newInstance(comic));
          }
        } else {
          showDismissableSnackbar(getString(R.string.err_bar_code_task));
          replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
        }
      });
  }
}
