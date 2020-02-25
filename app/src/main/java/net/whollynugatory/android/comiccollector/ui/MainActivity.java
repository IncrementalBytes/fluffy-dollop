/*
 * Copyright 2020 Ryan Ward
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

package net.whollynugatory.android.comiccollector.ui;

import android.Manifest;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import android.view.Menu;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import net.whollynugatory.android.comiccollector.PreferenceUtils;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.PathUtils;
import net.whollynugatory.android.comiccollector.common.RetrieveSeriesDataTask;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.entity.UserEntity;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;
import net.whollynugatory.android.comiccollector.ui.fragments.BarcodeScanFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.ComicBookFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.ComicListFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.ProductLookupFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.ResultListFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.SeriesFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.SeriesListFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.SyncFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.UserInputFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.UserPreferenceFragment;

public class MainActivity extends BaseActivity implements
  BarcodeScanFragment.OnBarcodeScanListener,
  ComicBookFragment.OnComicBookListener,
  ComicListFragment.OnComicListListener,
  ProductLookupFragment.OnProductLookupListener,
  ResultListFragment.OnResultListListener,
  SeriesFragment.OnSeriesListener,
  SeriesListFragment.OnSeriesListListener,
  SyncFragment.OnSyncListener,
  UserInputFragment.OnUserInputListener {

  private static final String TAG = BaseActivity.BASE_TAG + "MainActivity";

  private BottomNavigationView mNavigationView;
  private Toolbar mMainToolbar;
  private Snackbar mSnackbar;

  private StorageReference mStorage;

  private Bitmap mImageBitmap;
  private String mRemotePath;
  private int mRotationAttempts;
  private UserEntity mUser;

  /*
      AppCompatActivity Override(s)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_main);

    mMainToolbar = findViewById(R.id.main_toolbar);
    mNavigationView = findViewById(R.id.main_navigation);

    setSupportActionBar(mMainToolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {

      // update the title of the app based on which ever fragment we are showing
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
      if (fragment != null) {
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(UserPreferenceFragment.class.getName())) {
          setTitle(getString(R.string.title_preferences));
        } else if (fragmentClassName.equals(SyncFragment.class.getName())) {
          setTitle(getString(R.string.title_sync));
        } else {
          setTitle(getString(R.string.app_name));
        }
      }
    });

    mNavigationView.setOnNavigationItemSelectedListener(menuItem -> {

      Log.d(TAG, "++onNavigationItemSelectedListener(MenuItem)");
      switch (menuItem.getItemId()) {
        case R.id.navigation_recent:
          replaceFragment(ComicListFragment.newInstance());
          return true;
        case R.id.navigation_series:
          replaceFragment(SeriesListFragment.newInstance());
          return true;
        case R.id.navigation_settings:
          replaceFragment(UserPreferenceFragment.newInstance());
          return true;
      }

      return false;
    });

    mUser = new UserEntity();
    mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
    mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
    mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);
    // TODO: Update
//    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//    mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
//    mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
//    mUser.UseImageCapture = preferences.getBoolean(UserPreferenceFragment.USE_IMAGE_PREVIEW_PREFERENCE, false);
    if (UserEntity.isValid(mUser)) { // get most recent publisher and series data
      mRemotePath = PathUtils.combine(UserEntity.ROOT, mUser.Id, BaseActivity.DEFAULT_LIBRARY_FILE);
      mStorage = FirebaseStorage.getInstance().getReference().child(mRemotePath);
      checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_STORAGE_PERMISSIONS);
    } else {
      showDismissableSnackbar(getString(R.string.err_unknown_user));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    Log.d(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
    mUser = null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    Log.d(TAG, "++onOptionsItemSelected(MenuItem)");
    switch (item.getItemId()) {
      case R.id.action_home:
        replaceFragment(ComicListFragment.newInstance());
        break;
      case R.id.action_add:
        addComicBook();
        break;
      case R.id.action_settings:
        replaceFragment(UserPreferenceFragment.newInstance());
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
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    final String stringRef = savedInstanceState.getString("reference");
    if (stringRef == null) {
      return;
    }

    mStorage = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);
    List<FileDownloadTask> tasks = mStorage.getActiveDownloadTasks();
    if (tasks.size() > 0) {
      FileDownloadTask task = tasks.get(0);
      task.addOnSuccessListener(this, state -> showDismissableSnackbar(getString(R.string.message_export_success)));
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    if (mStorage != null) {
      outState.putString("reference", mStorage.toString());
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d(TAG, "++onActivityResult(int, int, Intent)");
    if (requestCode == BaseActivity.REQUEST_IMAGE_CAPTURE) {
      if (resultCode != RESULT_OK) {
        Log.d(TAG, "User canceled camera intent.");
      } else {
        File f = new File(getString(R.string.debug_path), data.getStringExtra(BaseActivity.ARG_DEBUG_FILE_NAME));
        Log.d(TAG, "Using " + f.getAbsolutePath());
        try {
          mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
          Crashlytics.logException(e);
        }

        if (mImageBitmap != null) {
          Bitmap emptyBitmap = Bitmap.createBitmap(
            mImageBitmap.getWidth(),
            mImageBitmap.getHeight(),
            mImageBitmap.getConfig());
          if (!mImageBitmap.sameAs(emptyBitmap)) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            View promptView = layoutInflater.inflate(R.layout.dialog_debug_image, null);
            ImageView imageView = promptView.findViewById(R.id.debug_dialog_image);
            BitmapDrawable bmd = new BitmapDrawable(this.getResources(), mImageBitmap);
            imageView.setImageDrawable(bmd);
            Builder alertDialogBuilder = new Builder(this);
            alertDialogBuilder.setView(promptView);
            alertDialogBuilder.setCancelable(false)
              .setPositiveButton("OK", (dialog, id) -> useFirebaseBarcodeScanning())
              .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

            android.app.AlertDialog alert = alertDialogBuilder.create();
            alert.show();
          } else {
            Log.w(TAG, getString(R.string.err_image_empty));
          }
        } else {
          Log.w(TAG, getString(R.string.err_image_not_found));
        }
      }
    } else {
      Log.w(TAG, "Unexpected activity request: " + requestCode);
      mNavigationView.setSelectedItemId(R.id.navigation_series);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    Log.d(TAG, "++onRequestPermissionsResult(int, String[], int[])");
    switch (requestCode) {
      case BaseActivity.REQUEST_CAMERA_PERMISSIONS:
        checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
        break;
      case BaseActivity.REQUEST_STORAGE_PERMISSIONS:
        checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_STORAGE_PERMISSIONS);
        break;
    }
  }

  /*
      Fragment Callback(s)
   */
  @Override
  public void onBarcodeManual() {

    Log.d(TAG, "++onBarcodeManual()");
    replaceFragment(UserInputFragment.newInstance());
  }

  @Override
  public void onBarcodeScanClose() {

    Log.d(TAG, "++onBarcodeScanClose()");
    replaceFragment(ComicListFragment.newInstance());
  }

  @Override
  public void onBarcodeScanned(String barcodeValue) {

    Log.d(TAG, "++onBarcodeScanned(String)");
    lookupBarcode(barcodeValue);
  }

  @Override
  public void onBarcodeScanSettings() {

    Log.d(TAG, "++onBarcodeScanSettings()");
    replaceFragment(UserPreferenceFragment.newInstance());
  }

  @Override
  public void onComicBookCancel() {

    Log.d(TAG, "++onComicBookCancel()");
    replaceFragment(ComicListFragment.newInstance());
  }

  @Override
  public void onComicBookSaved(ComicDetails comicDetails) {

    Log.d(TAG, "++onComicBookSaved(onComicBookSaved)");
    replaceFragment(ComicListFragment.newInstance());
  }

  @Override
  public void onComicListAddComicBook() {

    Log.d(TAG, "++onComicListAddComicBook()");
    addComicBook();
  }

  @Override
  public void onComicListEditComicBook(ComicDetails comicDetails) {

    Log.d(TAG, "++onComicListEditComicBook(ComicDetails)");
    replaceFragment(ComicBookFragment.newInstance(comicDetails));
  }

  @Override
  public void onComicListPopulated(int size) {

    Log.d(TAG, "++onComicListPopulated(int)");
    listPopulated(size);
  }

  @Override
  public void onProductLookupCancel() {

    Log.d(TAG, "++onProductLookupCancel()");
    replaceFragment(ComicListFragment.newInstance());
  }

  public void onProductLookupFound(ComicDetails comicDetails) {

    Log.d(TAG, "++onProductLookupFound(ComicDetail)");
    replaceFragment(UserInputFragment.newInstance(comicDetails));
  }

  @Override
  public void onProductLookupUnknown(SeriesDetails seriesDetails) {

    Log.d(TAG, "++onProductLookupUnknown(SeriesDetails)");
    new RetrieveSeriesDataTask(this, seriesDetails).execute();
  }

  @Override
  public void onResultListActionComplete(String message) {

    Log.d(TAG, "++onResultListActionComplete(String)");
    showDismissableSnackbar(message);
  }

  @Override
  public void onResultListItemSelected(ComicDetails comicDetails) {

    Log.d(TAG, "++onResultListItemSelected(ComicDetails)");
    replaceFragment(ComicBookFragment.newInstance(comicDetails));
  }

  @Override
  public void onSeriesCancel() {

    Log.d(TAG, "++onSeriesCancel()");
    replaceFragment(ComicListFragment.newInstance());
  }

  @Override
  public void onSeriesUpdated(ComicDetails comicDetails) {

    Log.d(TAG, "++onSeriesUpdated(ComicDetails)");
    replaceFragment(UserInputFragment.newInstance(comicDetails));
  }

  @Override
  public void onSeriesListAddComicBook() {

    Log.d(TAG, "++onSeriesListAddComicBook()");
    addComicBook();
  }

  @Override
  public void onSeriesListSelected(String seriesId) {

    Log.d(TAG, "++onSeriesListSelected(String)");
    // TODO:
  }

  @Override
  public void onSeriesListPopulated(int size) {

    Log.d(TAG, "++onSeriesListPopulated(int)");
  }

  // TODO: add comparison
  @Override
  public void onSyncExport() {

    Log.d(TAG, "++onSyncExport()");
    // TODO: move to fragment
//    mCollectorViewModel.exportable().observe(this, comicBookList -> {
//
//      if (comicBookList != null) {
//        FileOutputStream outputStream;
//        try {
//          outputStream = getApplicationContext().openFileOutput(BaseActivity.DEFAULT_EXPORT_FILE, Context.MODE_PRIVATE);
//          Gson gson = new Gson();
//          Type collectionType = new TypeToken<ArrayList<ComicBook>>() {}.getType();
//          ArrayList<ComicBook> booksWritten = new ArrayList<>(comicBookList);
//          outputStream.write(gson.toJson(booksWritten, collectionType).getBytes());
//          Log.d(TAG, "Comic books written: " + booksWritten.size());
//        } catch (Exception e) {
//          Log.w(TAG, "Exception when exporting local database.");
//          Crashlytics.logException(e);
//        }
//      }
//
//      try { // look for file output
//        InputStream stream = getApplicationContext().openFileInput(BaseActivity.DEFAULT_EXPORT_FILE);
//        UploadTask uploadTask = mStorage.putStream(stream);
//        uploadTask.addOnCompleteListener(task -> {
//
//          if (task.isSuccessful()) {
//            if (task.getResult() != null) {
//              showDismissableSnackbar(getString(R.string.message_export_success));
//            } else {
//              Log.w(TAG, "Storage task results were null; this is unexpected.");
//              showDismissableSnackbar(getString(R.string.err_storage_task_unexpected));
//            }
//          } else {
//            if (task.getException() != null) {
//              Log.e(TAG, "Could not export library.", task.getException());
//            }
//          }
//        });
//      } catch (FileNotFoundException fnfe) {
//        Log.w(TAG, "Could not export library.", fnfe);
//        Crashlytics.logException(fnfe);
//      } finally {
//        File tempFile = new File(getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
//        if (tempFile.exists()) {
//          if (tempFile.delete()) {
//            Log.d(TAG, "Removed temporary local export file.");
//          } else {
//            Log.w(TAG, "Temporary file was not removed.");
//          }
//        }
//      }
//    });
  }

  // TODO: add comparison
  @Override
  public void onSyncImport() {

    Log.d(TAG, "++onSyncImport()");
    File localFile = new File(getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
    FirebaseStorage.getInstance().getReference().child(mRemotePath).getFile(localFile).addOnCompleteListener(task -> {

      if (task.isSuccessful() && task.getException() == null) {
        File file = new File(getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
        Log.d(TAG, "Loading " + file.getAbsolutePath());
        if (file.exists() && file.canRead()) {
          try (Reader reader = new FileReader(file.getAbsolutePath())) {
            // TODO: move to fragment
//            mCollectorViewModel.deleteAllComicBooks();
            Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<ComicBookEntity>>() {}.getType();
            List<ComicBookEntity> comics = gson.fromJson(reader, collectionType);
            List<ComicBookEntity> updatedComics = new ArrayList<>();
            for (ComicBookEntity comicBook : comics) {
//              ComicBookEntity updated = new ComicBookEntity(comicBook);
//              updatedComics.add(updated);
            }

            // TODO: move to fragment
//            mCollectorViewModel.insertAll(updatedComics);
            showDismissableSnackbar(getString(R.string.status_sync_import_success));
          } catch (Exception e) {
            Log.w(TAG, "Failed reading local library.", e);
            Crashlytics.logException(e);
          } finally {
            if (file.delete()) { // remove temporary file
              Log.d(TAG, "Removed temporary local import file.");
            } else {
              Log.w(TAG, "Could not remove temporary file after importing.");
            }
          }
        } else {
          Log.d(TAG, "%s does not exist yet: " + BaseActivity.DEFAULT_EXPORT_FILE);
        }
      } else {
        if (task.getException() != null) {
          StorageException exception = (StorageException) task.getException();
          if (exception.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
            showDismissableSnackbar(getString(R.string.err_remote_library_not_found));
          } else {
            Log.e(TAG, "Could not import library.", task.getException());
            showDismissableSnackbar(getString(R.string.err_import_task));
          }
        } else {
          showDismissableSnackbar(getString(R.string.err_import_unknown));
        }
      }
    });
  }

  @Override
  public void onSyncFail() {

    Log.d(TAG, "++onSyncFail()");
    showDismissableSnackbar(getString(R.string.err_sync_unknown_user));
  }

  @Override
  public void onUserInputBookFound(ComicDetails comicDetails) {

    Log.d(TAG, "++onUserInputBookFound(ComicDetails)");
    replaceFragment(ComicListFragment.newInstance(comicDetails));
  }

  @Override
  public void onUserInputComplete(ComicDetails comicDetails) {

    Log.d(TAG, "++onUserInputComplete(ComicDetails)");
    replaceFragment(ComicBookFragment.newInstance(comicDetails));
  }

  @Override
  public void onUserInputRetry() {

    Log.d(TAG, "++onUserInputRetry()");
    checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
  }

  /*
    Public Method(s)
   */
  public void retrieveComicSeriesComplete(SeriesDetails seriesDetails) {

    Log.d(TAG, "++retrieveComicSeriesComplete(SeriesDetails)");
    if (seriesDetails.isValid()) {
      replaceFragment(SeriesFragment.newInstance(seriesDetails));
    } else {
      showDismissableSnackbar(getString(R.string.err_query_product_code));
    }
  }

  /*
      Private Method(s)
   */
  private void addComicBook() {

    Log.d(TAG, "++addComicBook()");
    checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
  }

  private void checkForPermission(String permissionName, int permissionId) {

    Log.d(TAG, "++checkForWritePermission(String, int)");
    if (ContextCompat.checkSelfPermission(this, permissionName) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionName)) {
        String requestMessage = getString(R.string.permission_default);
        switch (permissionId) {
          case BaseActivity.REQUEST_CAMERA_PERMISSIONS:
            requestMessage = getString(R.string.permission_camera);
            break;
          case BaseActivity.REQUEST_STORAGE_PERMISSIONS:
            requestMessage = getString(R.string.permission_storage);
            break;
        }

        Snackbar.make(
          findViewById(R.id.main_fragment_container),
          requestMessage,
          Snackbar.LENGTH_INDEFINITE)
          .setAction(
            getString(R.string.ok),
            view -> ActivityCompat.requestPermissions(
              MainActivity.this,
              new String[]{permissionName},
              permissionId))
          .show();
      } else {
        ActivityCompat.requestPermissions(this, new String[]{permissionName}, permissionId);
      }
    } else {
      Log.d(TAG, "Permission granted: " + permissionName);
      switch (permissionId) {
        case BaseActivity.REQUEST_CAMERA_PERMISSIONS:
          if (!PreferenceUtils.getUseCamera(this)) {
            replaceFragment(UserInputFragment.newInstance());
          } else {
            if (PreferenceUtils.getCameraBypass(this)) {
              LayoutInflater layoutInflater = LayoutInflater.from(this);
              View promptView = layoutInflater.inflate(R.layout.dialog_debug_camera, null);
              Spinner spinner = promptView.findViewById(R.id.debug_spinner_file);
              android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
              alertDialogBuilder.setView(promptView);
              alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                  Intent debugIntent = new Intent();
                  debugIntent.putExtra(BaseActivity.ARG_DEBUG_FILE_NAME, spinner.getSelectedItem().toString());
                  onActivityResult(BaseActivity.REQUEST_IMAGE_CAPTURE, RESULT_OK, debugIntent);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

              android.app.AlertDialog alert = alertDialogBuilder.create();
              alert.show();
            } else {
              replaceFragment(BarcodeScanFragment.newInstance());
            }
          }

          break;
        case BaseActivity.REQUEST_STORAGE_PERMISSIONS:
          replaceFragment(ComicListFragment.newInstance());
          break;
      }
    }
  }

  private void listPopulated(int size) {

    Log.d(TAG, "++listPopulated(int)");
    if (mMainToolbar != null && mMainToolbar.getMenu() != null) {
      MenuItem item = mMainToolbar.getMenu().findItem(R.id.action_add);
      if (item != null) {
        item.setEnabled(true);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_home);
      if (item != null) {
        item.setEnabled(true);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_settings);
      if (item != null) {
        item.setEnabled(true);
      }
    }

    if (size == 0) {
      if (mSnackbar == null || !mSnackbar.isShown()) {
        mSnackbar = Snackbar.make(
          findViewById(R.id.main_fragment_container),
          getString(R.string.err_no_data),
          Snackbar.LENGTH_LONG)
          .setAction(
            getString(R.string.add),
            view -> addComicBook());
        mSnackbar.show();
      }
    }
  }

  private void lookupBarcode(String barcodeValue) {

    Log.d(TAG, "++lookupBarcode(String)");
    Log.d(TAG, "Barcode: " + barcodeValue);
    replaceFragment(ProductLookupFragment.newInstance(barcodeValue));
  }

  private void replaceFragment(Fragment fragment) {

    Log.d(TAG, "++replaceFragment(Fragment)");
    getSupportFragmentManager()
      .beginTransaction()
      .replace(R.id.main_fragment_container, fragment)
      .addToBackStack(null)
      .commit();
  }

  private void showDismissableSnackbar(String message) {

    Log.w(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.main_fragment_container),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }

  private void useFirebaseBarcodeScanning() {

    Log.d(TAG, "++useFirebaseBarcodeScanning()");
    FirebaseVisionBarcodeDetectorOptions options =
      new FirebaseVisionBarcodeDetectorOptions.Builder()
        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_UPC_A, FirebaseVisionBarcode.FORMAT_UPC_E)
        .build();
    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

    com.google.android.gms.tasks.Task<java.util.List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
      .addOnCompleteListener(task -> {

        if (task.isSuccessful() && task.getResult() != null) {
          String barcodeValue = "";
          for (FirebaseVisionBarcode barcode : task.getResult()) {
            if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_PRODUCT) {
              barcodeValue = barcode.getDisplayValue();
              Log.d(TAG, "Found a bar code: " + barcodeValue);
            } else {
              Log.w(TAG, "Unexpected bar code: " + barcode.getDisplayValue());
            }
          }

          if (barcodeValue != null && !barcodeValue.isEmpty()) {
            mRotationAttempts = 0;
            lookupBarcode(barcodeValue);
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
            useFirebaseBarcodeScanning();
          } else {
            mRotationAttempts = 0;
            showDismissableSnackbar(getString(R.string.err_bar_code_not_found));
          }
        } else {
          showDismissableSnackbar(getString(R.string.err_bar_code_task_failed));
        }
      });
  }
}
