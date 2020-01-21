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

package net.whollynugatory.android.comiccollector.ui;

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
import net.whollynugatory.android.comiccollector.BuildConfig;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.common.ComicCollectorException;
import net.whollynugatory.android.comiccollector.common.PathUtils;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.views.ComicSeriesDetails;
import net.whollynugatory.android.comiccollector.db.entity.UserEntity;
import net.whollynugatory.android.comiccollector.ui.fragments.ItemListFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.ItemListFragment.ItemType;
import net.whollynugatory.android.comiccollector.ui.fragments.SyncFragment;
import net.whollynugatory.android.comiccollector.ui.fragments.UserPreferenceFragment;

public class MainActivity extends BaseActivity implements
  ItemListFragment.OnItemListListener,
  SyncFragment.OnSyncListener {

  private static final String TAG = BaseActivity.BASE_TAG + "MainActivity";

  private BottomNavigationView mNavigationView;
  private Toolbar mMainToolbar;
  private ProgressBar mProgress;
  private Snackbar mSnackbar;

  private StorageReference mStorage;

  private String mRemotePath;
  private String mTargetProductCode;
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
    mProgress = findViewById(R.id.main_progress);

    mProgress.setIndeterminate(true);
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
        case R.id.navigation_series:
          replaceFragment(ItemListFragment.newInstance(ItemType.Series));
          return true;
        case R.id.navigation_books:
          replaceFragment(ItemListFragment.newInstance());
          return true;
        case R.id.navigation_settings:
          replaceFragment(UserPreferenceFragment.newInstance());
          return true;
        case R.id.navigation_sync:
          replaceFragment(SyncFragment.newInstance(mUser));
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
        replaceFragment(ItemListFragment.newInstance());
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
    if (requestCode == BaseActivity.REQUEST_COMIC_ADD) {
      // TODO: pick up any change to tutorial?
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
            replaceFragment(ItemListFragment.newInstance(comicBook));
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
  public void onItemListAddComicBook() {

    Log.d(TAG, "++onItemListAddComicBook()");
    addComicBook();
  }

  @Override
  public void onItemListSeriesSelected(String series) {

    Log.d(TAG, "onItemListSeriesSelected()");
  }

  @Override
  public void onItemListCategorySelected(String category) {

    Log.d(TAG, "++onItemListCategorySelected(ComicBookEntity)");
  }

  @Override
  public void onItemListPopulated(int size) {

    Log.d(TAG, "++onItemListPopulated(int)");
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
    Public Method(s)
   */
  public void retrieveComicSeriesComplete(SeriesEntity seriesEntity) {

    Log.d(TAG, "++retrieveComicSeriesComplete(SeriesEntry)");
  }

  /*
      Private Method(s)
   */
  private void addComicBook() {

    Log.d(TAG, "++addComicBook()");
    // TODO: Update
//    Intent intent = new Intent(this, Mainctivity.class);
//    intent.putExtra(BaseActivity.ARG_USER, mUser);
//    startActivityForResult(intent, BaseActivity.REQUEST_COMIC_ADD);
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
