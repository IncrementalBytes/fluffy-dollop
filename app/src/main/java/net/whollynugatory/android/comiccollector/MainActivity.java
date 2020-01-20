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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;
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

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import net.whollynugatory.android.comiccollector.common.ComicCollectorException;
import net.whollynugatory.android.comiccollector.common.PathUtils;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.views.ComicBookDetails;
import net.whollynugatory.android.comiccollector.db.views.ComicSeriesDetails;
import net.whollynugatory.android.comiccollector.fragments.ComicBookFragment;
import net.whollynugatory.android.comiccollector.fragments.ComicBookListFragment;
import net.whollynugatory.android.comiccollector.fragments.ComicSeriesListFragment;
import net.whollynugatory.android.comiccollector.fragments.SyncFragment;
import net.whollynugatory.android.comiccollector.fragments.UserPreferenceFragment;
import net.whollynugatory.android.comiccollector.models.User;
import net.whollynugatory.android.comiccollector.fragments.ComicBookFragment.OnComicBookListener;
import net.whollynugatory.android.comiccollector.fragments.ComicBookListFragment.OnComicBookListListener;
import net.whollynugatory.android.comiccollector.fragments.ComicSeriesListFragment.OnComicSeriesListListener;
import net.whollynugatory.android.comiccollector.fragments.SyncFragment.OnSyncListener;
import net.whollynugatory.android.comiccollector.fragments.UserPreferenceFragment.OnPreferencesListener;

public class MainActivity extends BaseActivity implements
  OnComicBookListener,
  OnComicBookListListener,
  OnComicSeriesListListener,
  OnSyncListener,
  OnPreferencesListener {

  private static final String TAG = BaseActivity.BASE_TAG + "MainActivity";

  private BottomNavigationView mNavigationView;
  private Toolbar mMainToolbar;
  private ProgressBar mProgress;
  private Snackbar mSnackbar;

  private StorageReference mStorage;

  private String mRemotePath;
  private String mTargetProductCode;
  private User mUser;

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
    mProgress = findViewById(R.id.main_progress);

    mProgress.setIndeterminate(true);
    setSupportActionBar(mMainToolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {

      // update the title of the app based on which ever fragment we are showing
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
      if (fragment != null) {
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(ComicBookListFragment.class.getName())) {
          setTitle(getString(R.string.title_comic_library));
        } else if (fragmentClassName.equals(ComicSeriesListFragment.class.getName())) {
          setTitle(getString(R.string.title_series_library));
        } else if (fragmentClassName.equals(ComicBookFragment.class.getName())) {
          setTitle(getString(R.string.title_comic_book));
        } else if (fragmentClassName.equals(UserPreferenceFragment.class.getName())) {
          setTitle(getString(R.string.title_preferences));
        } else if (fragmentClassName.equals(SyncFragment.class.getName())) {
          setTitle(getString(R.string.title_sync));
        }
      }
    });

    mNavigationView.setOnNavigationItemSelectedListener(menuItem -> {

      Log.d(TAG, "++onNavigationItemSelectedListener(MenuItem)");
      switch (menuItem.getItemId()) {
        case R.id.navigation_series:
          replaceFragment(ComicSeriesListFragment.newInstance());
          return true;
        case R.id.navigation_books:
          if (mTargetProductCode != null && !mTargetProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE)) {
            replaceFragment(ComicBookListFragment.newInstance(mTargetProductCode));
            mTargetProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
          } else {
            replaceFragment(ComicBookListFragment.newInstance());
          }

          return true;
        case R.id.navigation_settings:
          replaceFragment(UserPreferenceFragment.newInstance(mUser));
          return true;
        case R.id.navigation_sync:
          replaceFragment(SyncFragment.newInstance(mUser));
          return true;
      }

      return false;
    });

    mUser = new User();
    mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
    mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
    mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
    mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
    mUser.UseImageCapture = preferences.getBoolean(UserPreferenceFragment.USE_IMAGE_PREVIEW_PREFERENCE, false);
    if (User.isValid(mUser)) { // get most recent publisher and series data
      mRemotePath = PathUtils.combine(User.ROOT, mUser.Id, BaseActivity.DEFAULT_LIBRARY_FILE);
      mStorage = FirebaseStorage.getInstance().getReference().child(mRemotePath);
      mNavigationView.setSelectedItemId(R.id.navigation_series);
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
        replaceFragment(ComicSeriesListFragment.newInstance());
        break;
      case R.id.action_add:
        addComicBook();
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
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (mStorage != null) {
      outState.putString("reference", mStorage.toString());
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d(TAG, "++onActivityResult(int, int, Intent)");
    if (requestCode == BaseActivity.REQUEST_COMIC_ADD) { // pick up any change to tutorial
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
      if (preferences.contains(UserPreferenceFragment.IS_GEEK_PREFERENCE)) {
        mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
      }

      if (preferences.contains(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE)) {
        mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
      }

      if (preferences.contains(UserPreferenceFragment.USE_IMAGE_PREVIEW_PREFERENCE)) {
        mUser.UseImageCapture = preferences.getBoolean(UserPreferenceFragment.USE_IMAGE_PREVIEW_PREFERENCE, false);
      }

      String message = null;
      ComicBookEntity comicBook = null;
      if (data != null) {
        if (data.hasExtra(BaseActivity.ARG_MESSAGE)) {
          message = data.getStringExtra(BaseActivity.ARG_MESSAGE);
        }

        if (data.hasExtra(BaseActivity.ARG_COMIC_BOOK)) {
          comicBook = (ComicBookEntity) data.getSerializableExtra(BaseActivity.ARG_COMIC_BOOK);
        }
      }

      switch (resultCode) {
        case RESULT_ADD_SUCCESS:
          if (comicBook != null) {
            replaceFragment(ComicBookFragment.newInstance(comicBook));
          } else {
            showDismissableSnackbar(getString(R.string.err_add_comic_book));
          }

          break;
        case RESULT_ADD_FAILED:
          if (message != null && message.length() > 0) {
            showDismissableSnackbar(message);
            mNavigationView.setSelectedItemId(R.id.navigation_series);
          } else {
            Log.e(TAG, "Activity return with incomplete data or no message was sent.");
            showDismissableSnackbar(getString(R.string.message_unknown_activity_result));
            mNavigationView.setSelectedItemId(R.id.navigation_series);
          }

          break;
        case RESULT_CANCELED:
          if (message != null && message.length() > 0) {
            showDismissableSnackbar(message);
            mNavigationView.setSelectedItemId(R.id.navigation_series);
          }

          break;
      }
    } else {
      Log.w(TAG, "Unexpected activity request: " + requestCode);
      mNavigationView.setSelectedItemId(R.id.navigation_series);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    Log.d(TAG, "++onRequestPermissionsResult(int, String[], int[])");
    checkForWritePermission();
  }

  /*
      Fragment Callback(s)
   */
  @Override
  public void onComicBookActionComplete(String message) {

    Log.d(TAG, "++onComicBookActionComplete(String)");
    showDismissableSnackbar(message);
  }

  @Override
  public void onComicBookAddedToLibrary(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++onComicBookAddedToLibrary(ComicBookEntity)");
    if (comicBookEntity != null) {
      // TODO: move to fragment
//      mCollectorViewModel.insert(comicBookEntity);
      mTargetProductCode = comicBookEntity.ProductCode;
      mNavigationView.setSelectedItemId(R.id.navigation_books);
    } else {
      showDismissableSnackbar(getString(R.string.err_add_comic_book));
    }
  }

  @Override
  public void onComicBookInit(boolean isSuccessful) {

    Log.d(TAG, "++onComicBookInit(boolean)");
    if (!isSuccessful) {
      showDismissableSnackbar(getString(R.string.err_comic_book_details));
    }
  }

  @Override
  public void onComicListActionComplete(String message) {

    Log.d(TAG, "++onComicListActionComplete(String)");
    showDismissableSnackbar(message);
  }

  @Override
  public void onComicListAddBook() {

    Log.d(TAG, "++onComicListAddBook()");
    addComicBook();
  }

  @Override
  public void onComicListDeleteBook() {

    Log.d(TAG, "onComicListDeleteBook()");
    mNavigationView.setSelectedItemId(R.id.navigation_series);
  }

  @Override
  public void onComicListItemSelected(ComicBookEntity comicBookEntity) {

    Log.d(TAG, "++onComicListItemSelected(ComicBookEntity)");
    replaceFragment(ComicBookFragment.newInstance(comicBookEntity));
  }

  @Override
  public void onComicListPopulated(int size) {

    Log.d(TAG, "++onComicListPopulated(int)");
    if (mProgress != null) {
      mProgress.setIndeterminate(false);
    }

    listPopulated(size);
  }

  @Override
  public void onPreferenceChanged() throws ComicCollectorException {

    Log.d(TAG, "++onPreferenceChanged()");
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    if (preferences.contains(UserPreferenceFragment.IS_GEEK_PREFERENCE)) {
      mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
    }

    if (preferences.contains(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE)) {
      mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
    }

    if (preferences.contains(UserPreferenceFragment.USE_IMAGE_PREVIEW_PREFERENCE)) {
      mUser.UseImageCapture = preferences.getBoolean(UserPreferenceFragment.USE_IMAGE_PREVIEW_PREFERENCE, false);
    }

    if (preferences.contains(UserPreferenceFragment.FORCE_EXCPETION_PREFERENCE)) {
      if (BuildConfig.DEBUG) {
        throw new ComicCollectorException("Testing the exceptional expection-ness");
      }
    }
  }

  @Override
  public void onSeriesListAddBook() {

    Log.d(TAG, "++onSeriesListAddBook()");
    checkForWritePermission();
  }

  @Override
  public void onSeriesListItemSelected(ComicSeriesDetails comicSeries) {

    Log.d(TAG, "++onSeriesListItemSelected(ComicSeriesDetails)");
    mTargetProductCode = comicSeries.Id;
    mNavigationView.setSelectedItemId(R.id.navigation_books);
  }

  @Override
  public void onSeriesListOnPopulated(int size) {

    Log.d(TAG, "++onSeriesListOnPopulated(int)");
    if (mProgress != null) {
      mProgress.setIndeterminate(false);
    }

    listPopulated(size);
  }

  // TODO: add comparison
  @Override
  public void onSyncExport() {

    Log.d(TAG, "++onSyncExport()");
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

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
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

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
              ComicBookEntity updated = new ComicBookEntity(comicBook);
              updated.parseProductCode(comicBook.Id);
              updatedComics.add(updated);
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

  /*
      Private Method(s)
   */
  private void addComicBook() {

    Log.d(TAG, "++addComicBook()");
    Intent intent = new Intent(this, AddActivity.class);
    intent.putExtra(BaseActivity.ARG_USER, mUser);
    startActivityForResult(intent, BaseActivity.REQUEST_COMIC_ADD);
  }

  private void checkForWritePermission() {

    Log.d(TAG, "++checkForWritePermission()");
    if (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission.WRITE_EXTERNAL_STORAGE)) {
        Snackbar.make(
          findViewById(R.id.main_fragment_container),
          getString(R.string.permission_storage),
          Snackbar.LENGTH_INDEFINITE)
          .setAction(
            getString(R.string.ok),
            view -> ActivityCompat.requestPermissions(
              MainActivity.this,
              new String[]{permission.WRITE_EXTERNAL_STORAGE},
              BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS))
          .show();
      } else {
        ActivityCompat.requestPermissions(
          this,
          new String[]{permission.WRITE_EXTERNAL_STORAGE},
          BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS);
      }
    } else {
      Log.d(TAG, "Permission granted: " + permission.WRITE_EXTERNAL_STORAGE);
      addComicBook();
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
    if (mProgress != null) {
      mProgress.setIndeterminate(false);
    }

    mSnackbar = Snackbar.make(
      findViewById(R.id.main_fragment_container),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }
}
