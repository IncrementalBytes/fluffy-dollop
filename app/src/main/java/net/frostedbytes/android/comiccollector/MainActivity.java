package net.frostedbytes.android.comiccollector;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
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
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.util.List;
import java.util.Locale;
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

  private static final String TAG = BASE_TAG + "MainActivity";

  private BottomNavigationView mNavigationView;
  private Toolbar mMainToolbar;
  private ProgressBar mProgress;
  private Snackbar mSnackbar;

  private StorageReference mStorage;

  private String mTargetSeriesId;
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
          if (mTargetSeriesId != null && !mTargetSeriesId.equals(BaseActivity.DEFAULT_PRODUCT_CODE)) {
            replaceFragment(ComicBookListFragment.newInstance(mTargetSeriesId));
            mTargetSeriesId = BaseActivity.DEFAULT_PRODUCT_CODE;
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
    if (User.isValid(mUser)) { // get most recent publisher and series data
      String remotePublishersPath = PathUtils.combine(BaseActivity.REMOTE_PATH, BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);
      File localFile = new File(getCacheDir(), BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);
      FirebaseStorage.getInstance().getReference().child(remotePublishersPath).getFile(localFile).addOnCompleteListener(task -> {

        if (task.isSuccessful()) {
          LogUtils.debug(TAG, "Retrieved %s from Firestore.", BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);
        } else {
          LogUtils.error(TAG, "Could not retrieve remote data.");
          if (task.getException() != null) {
            LogUtils.debug(TAG, "%s", task.getException().getMessage());
          }
        }

        mNavigationView.setSelectedItemId(R.id.navigation_series);
      });
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
        if (!mUser.Id.isEmpty() && !mUser.Id.equals(BaseActivity.DEFAULT_USER_ID)) {
          checkForWritePermission();
        } else {
          showDismissableSnackbar(getString(R.string.err_unknown_user));
        }

        break;
      case R.id.action_add:
        addComicBook();
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
      if (preferences.contains(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE)) {
        mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
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
      CollectorViewModel collectorViewModel = ViewModelProviders.of(this).get(CollectorViewModel.class);
      collectorViewModel.insert(comicBook);
      mTargetSeriesId = comicBook.SeriesId;
      mNavigationView.setSelectedItemId(R.id.navigation_books);
    } else {
      // TODO: handle error case
    }
  }

  @Override
  public void onComicBookInit(boolean isSuccessful) {

    LogUtils.debug(TAG, "++onComicBookInit(%s)", String.valueOf(isSuccessful));
    // TODO: add message if unsuccessful
  }

  @Override
  public void onComicBookRemoved(ComicBook comicBook) {

    LogUtils.debug(
      TAG,
      "++onComicBookRemoved(%s)",
      comicBook != null ? comicBook.toString() : "null");
    replaceFragment(ComicBookListFragment.newInstance());
  }

  @Override
  public void onComicListAddBook() {

    LogUtils.debug(TAG, "++onComicListAddBook()");
    addComicBook();
  }

  @Override
  public void onComicListItemSelected(ComicBookDetails comicBook) {

    LogUtils.debug(TAG, "++onComicListItemSelected(%s)", comicBook.toString());
    replaceFragment(ComicBookFragment.newInstance(comicBook));
  }

  @Override
  public void onComicListPopulated(int size) {

    LogUtils.debug(TAG, "++onComicListPopulated(%d)", size);
    if (mMainToolbar != null && mMainToolbar.getMenu() != null) {
      MenuItem item = mMainToolbar.getMenu().findItem(R.id.action_add);
      if (item != null) {
        item.setEnabled(true);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_home);
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

  @Override
  public void onPreferenceChanged() {

    LogUtils.debug(TAG, "++onPreferenceChanged()");
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    if (preferences.contains(UserPreferenceFragment.IS_GEEK_PREFERENCE)) {
      mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
    }

    if (preferences.contains(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE)) {
      mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
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
    mTargetSeriesId = comicSeries.Id;
    mNavigationView.setSelectedItemId(R.id.navigation_books);
  }

  // TODO: add comparison
  @Override
  public void onSyncExport() {

    LogUtils.debug(TAG, "++onSyncExport()");
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

    // TODO: rework export for books database
    showDismissableSnackbar(getString(R.string.err_export_task));
  }

  // TODO: add comparison
  @Override
  public void onSyncImport() {

    LogUtils.debug(TAG, "++onSyncImport()");
    if (mProgress != null) {
      mProgress.setIndeterminate(true);
    }

    // TODO: rework import to sync publisher and series data, and book library data
    showDismissableSnackbar(getString(R.string.err_import_task));
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
