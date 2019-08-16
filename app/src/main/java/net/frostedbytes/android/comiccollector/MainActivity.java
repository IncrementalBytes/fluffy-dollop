package net.frostedbytes.android.comiccollector;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import android.view.Menu;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.common.ReadLocalLibraryTask;
import net.frostedbytes.android.comiccollector.common.WriteToLocalLibraryTask;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesListFragment;
import net.frostedbytes.android.comiccollector.fragments.SyncFragment;
import net.frostedbytes.android.comiccollector.fragments.SystemMessageFragment;
import net.frostedbytes.android.comiccollector.fragments.UserPreferenceFragment;
import net.frostedbytes.android.comiccollector.models.ComicBook;
import net.frostedbytes.android.comiccollector.models.ComicPublisher;
import net.frostedbytes.android.comiccollector.models.ComicSeries;
import net.frostedbytes.android.comiccollector.models.User;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends BaseActivity implements
  ComicBookFragment.OnComicBookListener,
  ComicBookListFragment.OnComicBookListListener,
  ComicSeriesListFragment.OnComicSeriesListListener,
  SyncFragment.OnSyncListener,
  UserPreferenceFragment.OnPreferencesListener {

  private static final String TAG = BASE_TAG + "MainActivity";

  private BottomNavigationView mNavigationView;
  private Toolbar mMainToolbar;
  private Snackbar mSnackbar;

  private ListenerRegistration mComicPublishersRegistration;
  private ListenerRegistration mComicSeriesRegistration;
  private FirebaseFirestore mFirestore;
  private StorageReference mStorage;

  private ComicSeries mTargetSeries;
  private HashMap<String, ComicSeries> mComicSeries;
  private HashMap<String, ComicPublisher> mPublishers;
  private String mRemotePath;
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
    setSupportActionBar(mMainToolbar);
    getSupportFragmentManager().addOnBackStackChangedListener(() -> {

      // update the title of the app based on which ever fragment we are showing
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
      if (fragment != null) {
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(ComicBookListFragment.class.getName())) {
          setTitle(getString(R.string.title_comic_library));
        } else if (fragmentClassName.equals(ComicSeriesListFragment.class.getName())) {
          setTitle(getString(R.string.title_series_library));
        } else if (fragmentClassName.equals(ComicBookFragment.class.getName())) {
          setTitle(getString(R.string.title_comic_book));
          setTitle(getString(R.string.title_comic_book));
        } else if (fragmentClassName.equals(UserPreferenceFragment.class.getName())) {
          setTitle(getString(R.string.title_preferences));
        } else if (fragmentClassName.equals(SyncFragment.class.getName())) {
          setTitle(getString(R.string.title_sync));
        }
      }
    });

    mNavigationView = findViewById(R.id.main_navigation);
    mNavigationView.setOnNavigationItemSelectedListener(item -> {

      switch (item.getItemId()) {
        case R.id.navigation_series:
          replaceFragment(ComicSeriesListFragment.newInstance(mPublishers, mComicSeries));
          return true;
        case R.id.navigation_books:
          if (mTargetSeries != null) {
            replaceFragment(ComicBookListFragment.newInstance(mTargetSeries));
            mTargetSeries = null;
          } else {
            replaceFragment(ComicBookListFragment.newInstance(mComicSeries));
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

    mFirestore = FirebaseFirestore.getInstance();

    mComicSeries = new HashMap<>();
    mPublishers = new HashMap<>();

    mUser = new User();
    mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
    mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
    mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    mUser.IsGeek = preferences.getBoolean(UserPreferenceFragment.IS_GEEK_PREFERENCE, false);
    mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
    if (User.isValid(mUser)) { // do one-time firestore retrieve of publisher data
      mRemotePath = PathUtils.combine(User.ROOT, mUser.Id, BaseActivity.DEFAULT_LIBRARY_FILE);
      mStorage = FirebaseStorage.getInstance().getReference().child(mRemotePath);
      initialComicPublisherRead();
    } else {
      replaceFragment(SystemMessageFragment.newInstance(getString(R.string.err_unknown_user)));
    }
  }

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
  public boolean onCreateOptionsMenu(Menu menu) {

    LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    if (mComicPublishersRegistration != null) {
      mComicPublishersRegistration.remove();
    }

    if (mComicSeriesRegistration != null) {
      mComicSeriesRegistration.remove();
    }

    mFirestore = null;
    mComicSeries = null;
    mPublishers = null;
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
    String message = null;
    ComicSeries series = null;
    ComicBook book = null;
    if (data != null) {
      if (data.hasExtra(BaseActivity.ARG_MESSAGE)) {
        message = data.getStringExtra(BaseActivity.ARG_MESSAGE);
      }

      if (data.hasExtra(BaseActivity.ARG_COMIC_SERIES)) {
        series = data.getParcelableExtra(BaseActivity.ARG_COMIC_SERIES);
      }

      if (data.hasExtra(BaseActivity.ARG_COMIC_BOOK)) {
        book = data.getParcelableExtra(BaseActivity.ARG_COMIC_BOOK);
      }
    }

    if (requestCode == BaseActivity.REQUEST_COMIC_ADD) { // pick up any change to tutorial
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
      if (preferences.contains(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE)) {
        mUser.ShowBarcodeHint = preferences.getBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, true);
      }

      if (resultCode == RESULT_ADD_SUCCESS) {
        if (series != null && !mComicSeries.containsKey(series.getProductId())) {
          mComicSeries.put(series.getProductId(), series);
        }

        if (book != null && book.isValid()) {
          ComicPublisher comicPublisher = mPublishers.get(book.PublisherId);
          ComicSeries comicSeries = mComicSeries.get(book.getProductId());
          if (comicPublisher != null && comicSeries != null) {
            replaceFragment(ComicBookFragment.newInstance(book, comicPublisher, comicSeries, true));
          } else {
            LogUtils.warn(TAG, "Publisher and/or Series data is unexpected.");
            showDismissableSnackbar(getString(R.string.err_comic_add_fail));
            mNavigationView.setSelectedItemId(R.id.navigation_series);
          }
        }
      } else if (resultCode != RESULT_CANCELED) {
        if (message != null && message.length() > 0) {
          showDismissableSnackbar(message);
        } else {
          LogUtils.error(TAG, "Activity failed, but no message was sent.");
          showDismissableSnackbar(getString(R.string.message_unknown_activity_result));
        }

        mNavigationView.setSelectedItemId(R.id.navigation_series);
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

    if (comicBook == null) {
      LogUtils.debug(TAG, "++onComicBookAddedToLibrary(null)");
      showDismissableSnackbar(getString(R.string.err_add_comic_book));
    } else {
      LogUtils.debug(TAG, "++onComicBookAddedToLibrary(%s)", comicBook.toString());
      ComicSeries targetSeries = mComicSeries.get(comicBook.getProductId());
      if (targetSeries != null) {
        ArrayList<ComicBook> newBookList = new ArrayList<>();
        for (ComicBook book : targetSeries.ComicBooks) {
          if (!book.getFullId().equals(comicBook.getFullId())) {
            newBookList.add(book);
          }
        }

        newBookList.add(comicBook);
        targetSeries.ComicBooks = new ArrayList<>(newBookList);
        new WriteToLocalLibraryTask(this, new ArrayList<>(mComicSeries.values())).execute();
      } else {
        String message = String.format(
          Locale.US,
          "Comic not added, did not find %s in collection of ComicSeries.",
          comicBook.getProductId());
        LogUtils.warn(TAG, message);
        showDismissableSnackbar(message);
        mNavigationView.setSelectedItemId(R.id.navigation_series);
      }
    }
  }

  @Override
  public void onComicBookInit(boolean isSuccessful) {

    LogUtils.debug(TAG, "++onComicBookInit(%s)", String.valueOf(isSuccessful));
    // TODO: add message if unsuccessful
  }

  @Override
  public void onComicBookRemoved(ComicBook comicBook) {

    if (comicBook == null) {
      LogUtils.debug(TAG, "++onComicBookRemoved(null)");
      showDismissableSnackbar(getString(R.string.err_remove_comic_book));
    } else {
      LogUtils.debug(TAG, "++onComicBookRemoved(%s)", comicBook.toString());
      ComicSeries targetSeries = mComicSeries.get(comicBook.getProductId());
      if (targetSeries != null) {
        ArrayList<ComicBook> newBookList = new ArrayList<>();
        for (ComicBook book : targetSeries.ComicBooks) {
          if (!book.getFullId().equals(comicBook.getFullId())) {
            newBookList.add(book);
          }
        }

        targetSeries.ComicBooks = new ArrayList<>(newBookList);
        new WriteToLocalLibraryTask(this, new ArrayList<>(mComicSeries.values())).execute();
      } else {
        String message = String.format(
          Locale.US,
          "Comic not removed, did not find %s in collection of ComicSeries.",
          comicBook.getProductId());
        LogUtils.warn(TAG, message);
        showDismissableSnackbar(message);
        mNavigationView.setSelectedItemId(R.id.navigation_series);
      }
    }
  }

  @Override
  public void onComicListAddBook() {

    LogUtils.debug(TAG, "++onComicListAddBook()");
    addComicBook();
  }

  @Override
  public void onComicListItemSelected(ComicBook comicBook) {

    LogUtils.debug(TAG, "++onComicListItemSelected(%s)", comicBook.toString());
    ComicPublisher comicPublisher =mPublishers.get(comicBook.PublisherId);
    ComicSeries comicSeries = mComicSeries.get(comicBook.getProductId());
    if (comicPublisher != null && comicSeries != null) {
      replaceFragment(ComicBookFragment.newInstance(comicBook, comicPublisher, comicSeries));
    } else {
      LogUtils.warn(TAG, "Publisher and/or Series data is unexpected.");
    }
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
          findViewById(R.id.main_container),
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
    addComicBook();
  }

  @Override
  public void onSeriesListItemSelected(ComicSeries series) {

    LogUtils.debug(TAG, "++onSeriesListItemSelected(%s)", series.toString());
    mTargetSeries = series;
    mNavigationView.setSelectedItemId(R.id.navigation_books);
  }

  @Override
  public void onSeriesListPopulated(int size) {

    LogUtils.debug(TAG, "++onSeriesListPopulated(%d)", size);
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
          findViewById(R.id.main_container),
          getString(R.string.err_no_data),
          Snackbar.LENGTH_LONG)
          .setAction(
            getString(R.string.add),
            view -> addComicBook());
        mSnackbar.show();
      }
    }
  }

  // TODO: add comparison
  @Override
  public void onSyncExport() {

    LogUtils.debug(TAG, "++onSyncExport()");
    try {
      InputStream stream = getApplicationContext().openFileInput(BaseActivity.DEFAULT_LIBRARY_FILE);
      UploadTask uploadTask = mStorage.putStream(stream);
      uploadTask.addOnCompleteListener(task -> {

        if (task.isSuccessful()) {
          if (task.getResult() != null) {
            if (task.getResult().getMetadata() != null) {
              mNavigationView.setSelectedItemId(R.id.navigation_series);
              showDismissableSnackbar(getString(R.string.message_export_success));
            }
          } else {
            LogUtils.warn(TAG, "Storage task results were null; this is unexpected.");
            showDismissableSnackbar(getString(R.string.err_storage_task_unexpected));
          }
        } else {
          if (task.getException() != null) {
            LogUtils.error(TAG, "Could not export library: %s", task.getException().getMessage());
          }

          showDismissableSnackbar(getString(R.string.err_export_task));
        }
      });
    } catch (FileNotFoundException fe) {
      LogUtils.warn(TAG, fe.getMessage());
      showDismissableSnackbar(getString(R.string.err_export));
    }
  }

  // TODO: add comparison
  @Override
  public void onSyncImport() {

    LogUtils.debug(TAG, "++onSyncImport()");
    File localFile = new File(getFilesDir(), BaseActivity.DEFAULT_LIBRARY_FILE);
    FirebaseStorage.getInstance().getReference().child(mRemotePath).getFile(localFile).addOnCompleteListener(task -> {

      if (task.isSuccessful() && task.getException() == null) {
        new ReadLocalLibraryTask(this, getFilesDir()).execute();
      } else {
        if (task.getException() != null) {
          StorageException exception = (StorageException) task.getException();
          if (exception.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
            showDismissableSnackbar(getString(R.string.err_remote_library_not_found));
          } else {
            LogUtils.error(TAG, "Could not import library: %s", task.getException().getMessage());
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
      Public Method(s)
   */
  public void retrieveLocalLibraryComplete(ArrayList<ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++retrieveLocalLibraryComplete(%d)", comicBooks.size());
    for (ComicBook comicBook : comicBooks) {
      if (mComicSeries.containsKey(comicBook.getProductId())) {
        mComicSeries.get(comicBook.getProductId()).ComicBooks.add(comicBook);
      }
    }

    mNavigationView.setSelectedItemId(R.id.navigation_series);
  }

  public void writeLibraryComplete(ArrayList<ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++writeLibraryComplete(%d)", comicBooks.size());
    mNavigationView.setSelectedItemId(R.id.navigation_series);
  }

  /*
      Private Method(s)
   */

  private void addComicBook() {

    LogUtils.debug(TAG, "++addComicBook()");
    Intent intent = new Intent(this, AddActivity.class);
    intent.putExtra(BaseActivity.ARG_USER, mUser);
    intent.putExtra(BaseActivity.ARG_COMIC_PUBLISHERS, mPublishers);
    intent.putExtra(BaseActivity.ARG_COMIC_SERIES, mComicSeries);
    startActivityForResult(intent, BaseActivity.REQUEST_COMIC_ADD);
  }

  private void checkForWritePermission() {

    LogUtils.debug(TAG, "++checkForWritePermission()");
    if (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission.WRITE_EXTERNAL_STORAGE)) {
        Snackbar.make(
          findViewById(R.id.main_container),
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
      new ReadLocalLibraryTask(this, getFilesDir()).execute();
    }
  }


  private void handlePublisherDocumentChange(DocumentChange dc) {

    ComicPublisher publisher = dc.getDocument().toObject(ComicPublisher.class);
    publisher.Id = dc.getDocument().getId();
    switch (dc.getType()) {
      case ADDED:
      case MODIFIED:
        if (publisher.isValid()) {
          if (mPublishers.containsKey(publisher.Id)) {
            ComicPublisher targetPublisher = mPublishers.get(publisher.Id);
            if (targetPublisher != null) {
              ComicPublisher oldPublisher = new ComicPublisher(targetPublisher);
              mPublishers.replace(publisher.Id, oldPublisher, publisher);
              LogUtils.debug(
                TAG,
                "%s publisher: %s",
                dc.getType() == DocumentChange.Type.ADDED ? "Added" : "Modified",
                publisher.toString());
            } else {
              LogUtils.warn(TAG, "Failed to retrieve Comic Publisher from collection: %s", publisher.toString());
            }
          } else {
            mPublishers.put(publisher.Id, publisher);
            LogUtils.debug(
              TAG,
              "%s publisher: %s",
              dc.getType() == DocumentChange.Type.ADDED ? "Added" : "Modified",
              publisher.toString());
          }
        } else {
          LogUtils.warn(TAG, "Comic Publisher is unknown.");
        }

        break;
      case REMOVED:
        LogUtils.debug(TAG, "Removed publisher: %s", publisher.toString());
        mPublishers.remove(publisher.Id);
        break;
    }
  }

  private void handleSeriesDocumentChange(DocumentChange dc) {

    ComicSeries series = dc.getDocument().toObject(ComicSeries.class);
    series.parseProductCode(dc.getDocument().getId());
    switch (dc.getType()) {
      case ADDED:
      case MODIFIED:
        if (series.isValid()) {
          if (mComicSeries.containsKey(series.getProductId())) {
            ComicSeries targetSeries = mComicSeries.get(series.getProductId());
            if (targetSeries != null) {
              ComicSeries oldSeries = new ComicSeries(targetSeries);
              mComicSeries.replace(series.getProductId(), oldSeries, series);
              LogUtils.debug(
                TAG,
                "%s series: %s",
                dc.getType() == DocumentChange.Type.ADDED ? "Added" : "Modified",
                series.toString());
            } else {
              LogUtils.warn(TAG, "Failed to retrieve Comic Series from collection: %s", series.toString());
            }
          } else {
            mComicSeries.put(series.getProductId(), series);
            LogUtils.debug(
              TAG,
              "%s series: %s",
              dc.getType() == DocumentChange.Type.ADDED ? "Added" : "Modified",
              series.toString());
          }
        } else {
          LogUtils.warn(TAG, "Comic Series is unknown: %s", series.toString());
        }

        break;
      case REMOVED:
        LogUtils.debug(TAG, "Removed series: %s", series.toString());
        mComicSeries.remove(series.getProductId());
        break;
    }
  }

  private void initialComicPublisherRead() {

    LogUtils.debug(TAG, "++initialComicPublisherRead()");
    mPublishers = new HashMap<>();
    mFirestore.collection(ComicPublisher.ROOT).whereEqualTo("IsFlagged", false).get().addOnCompleteListener(task -> {

        if (task.isSuccessful()) {
          if (task.getResult() != null) {
            for (DocumentChange dc : task.getResult().getDocumentChanges()) {
              handlePublisherDocumentChange(dc);
            }

            initialComicSeriesRead();
          } else {
            replaceFragment(SystemMessageFragment.newInstance(getString(R.string.err_publisher_data_empty)));
          }
        } else {
          replaceFragment(SystemMessageFragment.newInstance(getString(R.string.err_publisher_task_failed)));
        }
      });
  }

  private void initialComicSeriesRead() {

    LogUtils.debug(TAG, "++initialComicSeriesRead()");
    mComicSeries = new HashMap<>();
    mFirestore.collection(ComicSeries.ROOT).get().addOnCompleteListener(task -> {

      if (task.isSuccessful()) {
        if (task.getResult() != null) {
          for (DocumentChange dc : task.getResult().getDocumentChanges()) {
            handleSeriesDocumentChange(dc);
          }

          // series data is loaded, proceed to loading comic library data
          checkForWritePermission();

          // now setup listeners
          listenToServerComicPublishers();
          listenToServerComicSeries();
        } else {
          replaceFragment(SystemMessageFragment.newInstance(getString(R.string.err_series_data_empty)));
        }
      } else {
        replaceFragment(SystemMessageFragment.newInstance(getString(R.string.err_series_task_failed)));
      }
    });
  }

  private void listenToServerComicPublishers() {

    LogUtils.debug(TAG, "++listenToServerComicPublishers()");
    mComicPublishersRegistration = mFirestore.collection(ComicPublisher.ROOT).addSnapshotListener((snapshots, ffe) -> {

      if (ffe != null) {
        LogUtils.warn(TAG, "Comic Publishers listen error: %s", ffe.getMessage());
        return;
      }

      if (snapshots == null) {
        LogUtils.error(TAG, "Comic Publisher snapshot was null.");
        return;
      }

      for (DocumentChange dc : snapshots.getDocumentChanges()) {
        handlePublisherDocumentChange(dc);
      }
    });
  }

  private void listenToServerComicSeries() {

    LogUtils.debug(TAG, "++listenToServerComicSeries()");
    mComicSeriesRegistration = mFirestore.collection(ComicSeries.ROOT).addSnapshotListener((snapshots, ffe) -> {

      if (ffe != null) {
        LogUtils.warn(TAG, "Comic Series listen error: %s", ffe.getMessage());
        return;
      }

      if (snapshots == null) {
        LogUtils.error(TAG, "Comic Series snapshot was null.");
        return;
      }

      for (DocumentChange dc : snapshots.getDocumentChanges()) {
        handleSeriesDocumentChange(dc);
      }
    });
  }

  private void replaceFragment(Fragment fragment) {

    LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.main_container, fragment);
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    fragmentTransaction.addToBackStack(fragment.getClass().getName());
    fragmentTransaction.commitAllowingStateLoss();
  }

  private void showDismissableSnackbar(String message) {

    LogUtils.warn(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.main_container),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }
}
