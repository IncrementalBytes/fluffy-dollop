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
package net.frostedbytes.android.comiccollector;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
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
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.common.ComicCollectorException;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.db.entity.ComicBook;
import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesListFragment;
import net.frostedbytes.android.comiccollector.fragments.SyncFragment;
import net.frostedbytes.android.comiccollector.fragments.UserPreferenceFragment;
import net.frostedbytes.android.comiccollector.models.User;
import net.frostedbytes.android.comiccollector.viewmodel.CollectorViewModel;

public class MainActivity extends BaseActivity implements
  ComicBookFragment.OnComicBookListener,
  ComicBookListFragment.OnComicBookListListener,
  ComicSeriesListFragment.OnComicSeriesListListener,
  SyncFragment.OnSyncListener,
  UserPreferenceFragment.OnPreferencesListener {

  private static final String TAG = BaseActivity.BASE_TAG + "MainActivity";

  private BottomNavigationView mNavigationView;
  private Toolbar mMainToolbar;
  private ProgressBar mProgress;
  private Snackbar mSnackbar;

  private CollectorViewModel mCollectorViewModel;

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

    LogUtils.debug(TAG, "++onCreate(Bundle)");
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

      LogUtils.debug(TAG, "++onNavigationItemSelectedListener(%s)", menuItem.getTitle());
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
      mCollectorViewModel = ViewModelProviders.of(this).get(CollectorViewModel.class);
      mRemotePath = PathUtils.combine(User.ROOT, mUser.Id, BaseActivity.DEFAULT_LIBRARY_FILE);
      mStorage = FirebaseStorage.getInstance().getReference().child(mRemotePath);
      mNavigationView.setSelectedItemId(R.id.navigation_series);
    } else {
      showDismissableSnackbar(getString(R.string.err_unknown_user));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    mUser = null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    LogUtils.debug(TAG, "++onOptionsItemSelected(%s)", item.getTitle());
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

    LogUtils.debug(TAG, "++onActivityResult(%d, %d, Intent)", requestCode, resultCode);
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
      ComicBookDetails comicBook = null;
      if (data != null) {
        if (data.hasExtra(BaseActivity.ARG_MESSAGE)) {
          message = data.getStringExtra(BaseActivity.ARG_MESSAGE);
        }

        if (data.hasExtra(BaseActivity.ARG_COMIC_BOOK)) {
          comicBook = (ComicBookDetails)data.getSerializableExtra(BaseActivity.ARG_COMIC_BOOK);
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
            LogUtils.error(TAG, "Activity return with incomplete data or no message was sent.");
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
      LogUtils.warn(TAG, String.format(Locale.US, "Unexpected activity request: %d", requestCode));
      mNavigationView.setSelectedItemId(R.id.navigation_series);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    LogUtils.debug(TAG, "++onRequestPermissionsResult(int, String[], int[])");
    checkForWritePermission();
  }

  /*
      Fragment Callback(s)
   */
  @Override
  public void onComicBookActionComplete(String message) {

    LogUtils.debug(TAG, "++onComicBookActionComplete(%s)", message);
    showDismissableSnackbar(message);
  }

  @Override
  public void onComicBookAddedToLibrary(ComicBook comicBook) {

    LogUtils.debug(
      TAG,
      "++onComicBookAddedToLibrary(%s)",
      comicBook != null ? comicBook.toString() : "null");
    if (comicBook != null) {
      mCollectorViewModel.insert(comicBook);
      mTargetProductCode = comicBook.ProductCode;
      mNavigationView.setSelectedItemId(R.id.navigation_books);
    } else {
      showDismissableSnackbar(getString(R.string.err_add_comic_book));
    }
  }

  @Override
  public void onComicBookInit(boolean isSuccessful) {

    LogUtils.debug(TAG, "++onComicBookInit(%s)", String.valueOf(isSuccessful));
    if (!isSuccessful) {
      showDismissableSnackbar(getString(R.string.err_comic_book_details));
    }
  }

  @Override
  public void onComicListActionComplete(String message) {

    LogUtils.debug(TAG, "++onComicListActionComplete(%s)", message);
    showDismissableSnackbar(message);
  }

  @Override
  public void onComicListAddBook() {

    LogUtils.debug(TAG, "++onComicListAddBook()");
    addComicBook();
  }

  @Override
  public void onComicListDeleteBook() {

    LogUtils.debug(TAG, "onComicListDeleteBook()");
    mNavigationView.setSelectedItemId(R.id.navigation_series);
  }

  @Override
  public void onComicListItemSelected(ComicBookDetails comicBook) {

    LogUtils.debug(TAG, "++onComicListItemSelected(%s)", comicBook.toString());
    replaceFragment(ComicBookFragment.newInstance(comicBook));
  }

  @Override
  public void onComicListPopulated(int size) {

    LogUtils.debug(TAG, "++onComicListPopulated(%d)", size);
    if (mProgress != null) {
      mProgress.setIndeterminate(false);
    }

    listPopulated(size);
  }

  @Override
  public void onPreferenceChanged() throws ComicCollectorException {

    LogUtils.debug(TAG, "++onPreferenceChanged()");
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

    LogUtils.debug(TAG, "++onSeriesListAddBook()");
    checkForWritePermission();
  }

  @Override
  public void onSeriesListItemSelected(ComicSeriesDetails comicSeries) {

    LogUtils.debug(TAG, "++onSeriesListItemSelected(%s)", comicSeries.toString());
    mTargetProductCode = comicSeries.Id;
    mNavigationView.setSelectedItemId(R.id.navigation_books);
  }

  @Override
  public void onSeriesListOnPopulated(int size) {

    LogUtils.debug(TAG, "++onSeriesListOnPopulated(%d)", size);
    if (mProgress != null) {
      mProgress.setIndeterminate(false);
    }

    listPopulated(size);
  }

  // TODO: add comparison
  @Override
  public void onSyncExport() {

    LogUtils.debug(TAG, "++onSyncExport()");
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

    mCollectorViewModel.exportable().observe(this, comicBookList -> {

      if (comicBookList != null) {
        FileOutputStream outputStream;
        try {
          outputStream = getApplicationContext().openFileOutput(BaseActivity.DEFAULT_EXPORT_FILE, Context.MODE_PRIVATE);
          Gson gson = new Gson();
          Type collectionType = new TypeToken<ArrayList<ComicBook>>() {}.getType();
          ArrayList<ComicBook> booksWritten = new ArrayList<>(comicBookList);
          outputStream.write(gson.toJson(booksWritten, collectionType).getBytes());
          LogUtils.debug(TAG, "Wrote %d comic books to %s", booksWritten.size(), BaseActivity.DEFAULT_EXPORT_FILE);
        } catch (Exception e) {
          LogUtils.warn(TAG, "Exception when exporting local database.");
          Crashlytics.logException(e);
        }
      }

      try { // look for file output
        InputStream stream = getApplicationContext().openFileInput(BaseActivity.DEFAULT_EXPORT_FILE);
        UploadTask uploadTask = mStorage.putStream(stream);
        uploadTask.addOnCompleteListener(task -> {

          if (task.isSuccessful()) {
            if (task.getResult() != null) {
              showDismissableSnackbar(getString(R.string.message_export_success));
            } else {
              LogUtils.warn(TAG, "Storage task results were null; this is unexpected.");
              showDismissableSnackbar(getString(R.string.err_storage_task_unexpected));
            }
          } else {
            if (task.getException() != null) {
              LogUtils.error(TAG, "Could not export library.", task.getException());
            }
          }
        });
      } catch (FileNotFoundException fnfe) {
        LogUtils.warn(TAG, "Could not export library.", fnfe);
        Crashlytics.logException(fnfe);
      } finally {
        File tempFile = new File(getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
        if (tempFile.exists()) {
          if (tempFile.delete()) {
            LogUtils.debug(TAG, "Removed temporary local export file.");
          } else {
            LogUtils.warn(TAG, "Unable t");
          }
        }
      }
    });
  }

  // TODO: add comparison
  @Override
  public void onSyncImport() {

    LogUtils.debug(TAG, "++onSyncImport()");
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

    File localFile = new File(getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
    FirebaseStorage.getInstance().getReference().child(mRemotePath).getFile(localFile).addOnCompleteListener(task -> {

      if (task.isSuccessful() && task.getException() == null) {
        File file = new File(getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
        LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
        if (file.exists() && file.canRead()) {
          try (Reader reader = new FileReader(file.getAbsolutePath())) {
            mCollectorViewModel.deleteAllComicBooks();
            Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<ComicBook>>() {}.getType();
            List<ComicBook> comics = gson.fromJson(reader, collectionType);
            List<ComicBook> updatedComics = new ArrayList<>();
            for (ComicBook comicBook : comics) {
              ComicBook updated = new ComicBook(comicBook);
              updated.parseProductCode(comicBook.Id);
              if (updated.isValid()) {
                updatedComics.add(updated);
              }
            }

            mCollectorViewModel.insertAll(updatedComics);
            showDismissableSnackbar(getString(R.string.status_sync_import_success));
          } catch (Exception e) {
            LogUtils.warn(TAG, "Failed reading local library.", e);
            Crashlytics.logException(e);
          } finally {
            if (file.delete()) { // remove temporary file
              LogUtils.debug(TAG, "Removed temporary local import file.");
            } else {
              LogUtils.warn(TAG, "Could not remove temporary file after importing.");
            }
          }
        } else {
          LogUtils.debug(TAG, "%s does not exist yet.", BaseActivity.DEFAULT_EXPORT_FILE);
        }
      } else {
        if (task.getException() != null) {
          StorageException exception = (StorageException) task.getException();
          if (exception.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
            showDismissableSnackbar(getString(R.string.err_remote_library_not_found));
          } else {
            LogUtils.error(TAG, "Could not import library.", task.getException());
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

    LogUtils.debug(TAG, "++onSyncFail()");
    showDismissableSnackbar(getString(R.string.err_sync_unknown_user));
  }

  /*
      Private Method(s)
   */
  private void addComicBook() {

    LogUtils.debug(TAG, "++addComicBook()");
    Intent intent = new Intent(this, AddActivity.class);
    intent.putExtra(BaseActivity.ARG_USER, mUser);
    startActivityForResult(intent, BaseActivity.REQUEST_COMIC_ADD);
  }

  private void checkForWritePermission() {

    LogUtils.debug(TAG, "++checkForWritePermission()");
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
      LogUtils.debug(TAG, "%s permission granted.", permission.WRITE_EXTERNAL_STORAGE);
      addComicBook();
    }
  }

  private void listPopulated(int size) {

    LogUtils.debug(TAG, "++listPopulated(%d)", size);
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

    LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
    getSupportFragmentManager()
      .beginTransaction()
      .replace(R.id.main_fragment_container, fragment)
      .addToBackStack(null)
      .commit();
  }

  private void showDismissableSnackbar(String message) {

    LogUtils.warn(TAG, message);
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
