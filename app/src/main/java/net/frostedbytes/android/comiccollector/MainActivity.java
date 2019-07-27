package net.frostedbytes.android.comiccollector;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.crashlytics.android.Crashlytics;
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
import android.widget.ProgressBar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.WriteToLocalLibraryTask;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesListFragment;
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
  UserPreferenceFragment.OnPreferencesListener {

  private static final String TAG = BASE_TAG + "MainActivity";

  private Toolbar mMainToolbar;
  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

  private ListenerRegistration mComicPublishersRegistration;
  private ListenerRegistration mComicSeriesRegistration;
  private FirebaseFirestore mFirestore;

  private HashMap<String, ComicSeries> mComicSeries;
  private HashMap<String, ComicPublisher> mPublishers;
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

    mMainToolbar = findViewById(R.id.main_toolbar);
    setSupportActionBar(mMainToolbar);

    mProgressBar = findViewById(R.id.main_progress);
    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
      if (fragment != null) {
        updateTitle(fragment);
      }
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
    if (User.isValid(mUser)) {
      mProgressBar.setIndeterminate(true);

      // do one-time firestore retrieve of publisher data
      initialComicPublisherRead();
    } else {
      replaceFragment(SystemMessageFragment.newInstance(getString(R.string.err_unknown_user)));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.menu_main, menu);
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
      case R.id.action_sync:
        syncComicBooks();
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
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    LogUtils.debug(TAG, "++onSaveInstanceState(Bundle)");
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

    switch (requestCode) {
      case BaseActivity.REQUEST_COMIC_ADD:
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
            }
          }
        } else {
          if (message != null && message.length() > 0) {
            showDismissableSnackbar(message);
          } else {
            LogUtils.error(TAG, "Activity failed, but no message was sent.");
            showDismissableSnackbar(getString(R.string.message_unknown_activity_result));
          }

          replaceFragment(ComicSeriesListFragment.newInstance(mPublishers, mComicSeries));
        }
        break;
      case BaseActivity.REQUEST_SYNC:
        switch (resultCode) {
          case BaseActivity.RESULT_SYNC_FAILED:
            if (message != null && message.length() > 0) {
              showDismissableSnackbar(message);
            } else {
              LogUtils.error(TAG, "AddActivity failed, but no message was sent.");
              showDismissableSnackbar(getString(R.string.message_unknown_activity_result));
            }
            break;
          case BaseActivity.RESULT_IMPORT_SUCCESS:
            readLocalLibrary(getFilesDir());
            replaceFragment(ComicSeriesListFragment.newInstance(mPublishers, mComicSeries));
            break;
          case BaseActivity.RESULT_EXPORT_SUCCESS:
            showDismissableSnackbar(getString(R.string.message_export_success));
            break;
        }

        break;
      default:
        LogUtils.warn(TAG, String.format(Locale.US, "Unexpected activity result: %d", requestCode));
        break;
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
        replaceFragment(ComicSeriesListFragment.newInstance(mPublishers, mComicSeries));
      }
    }
  }

  @Override
  public void onComicBookInit(boolean isSuccessful) {

    LogUtils.debug(TAG, "++onComicBookInit(%s)", String.valueOf(isSuccessful));
    // TODO: add message if unsuccessful
    mProgressBar.setIndeterminate(false);
  }

  @Override
  public void onComicBookRemoved(ComicBook comicBook) {

    mProgressBar.setIndeterminate(false);
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
        replaceFragment(ComicSeriesListFragment.newInstance(mPublishers, mComicSeries));
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
    mProgressBar.setIndeterminate(false);
    if (mMainToolbar != null && mMainToolbar.getMenu() != null) {
      MenuItem item = mMainToolbar.getMenu().findItem(R.id.action_add);
      if (item != null) {
        item.setEnabled(true);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_home);
      if (item != null) {
        item.setEnabled(true);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_sync);
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
  public void onComicListSynchronize() {

    LogUtils.debug(TAG, "++onComicListSynchronize()");
    mProgressBar.setIndeterminate(true);
    syncComicBooks();
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

  @Override
  public void onSeriesListAddBook() {

    LogUtils.debug(TAG, "++onSeriesListAddBook()");
    addComicBook();
  }

  @Override
  public void onSeriesListItemSelected(ComicSeries series) {

    LogUtils.debug(TAG, "++onSeriesListItemSelected(%s)", series.toString());
    replaceFragment(ComicBookListFragment.newInstance(series));
  }

  @Override
  public void onSeriesListPopulated(int size) {

    LogUtils.debug(TAG, "++onSeriesListPopulated()");
    mProgressBar.setIndeterminate(false);
    if (mMainToolbar != null && mMainToolbar.getMenu() != null) {
      MenuItem item = mMainToolbar.getMenu().findItem(R.id.action_add);
      if (item != null) {
        item.setEnabled(true);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_home);
      if (item != null) {
        item.setEnabled(true);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_sync);
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
  public void onSeriesListSynchronize() {

    LogUtils.debug(TAG, "++onSeriesListSynchronize()");
    mProgressBar.setIndeterminate(true);
    syncComicBooks();
  }

  /*
      Public Method(s)
   */
  public void writeLibraryComplete(ArrayList<ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++writeLibraryComplete(%d)", comicBooks.size());
    mProgressBar.setIndeterminate(false);
    replaceFragment(ComicSeriesListFragment.newInstance(mPublishers, mComicSeries));
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
      readLocalLibrary(getFilesDir());
      replaceFragment(ComicSeriesListFragment.newInstance(mPublishers, mComicSeries));
    }
  }


  private void handlePublisherDocumentChange(DocumentChange dc) {

    ComicPublisher publisher = dc.getDocument().toObject(ComicPublisher.class);
    publisher.Id = dc.getDocument().getId();
    switch (dc.getType()) {
      case ADDED:
        LogUtils.debug(TAG, "New publisher: %s", publisher.toString());
        mPublishers.put(publisher.Id, publisher);
        break;
      case MODIFIED:
        LogUtils.debug(TAG, "Modified publisher: %s", publisher.toString());
        mPublishers.put(publisher.Id, publisher);
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
        LogUtils.debug(TAG, "New series: %s", series.toString());
        if (!series.PublisherId.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID) ||
          !series.Id.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID)) {
          mComicSeries.put(series.getProductId(), series);
        } else {
          LogUtils.error(TAG, "New series publisher is unknown: %s", series.getProductId());
        }

        break;
      case MODIFIED:
        LogUtils.debug(TAG, "Modified series: %s", series.toString());
        mComicSeries.put(series.getProductId(), series);
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
    mFirestore.collection(ComicPublisher.ROOT).get().addOnCompleteListener(task -> {

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

  /**
   * Looks for local copy of library in the user's application data.
   * @param fileDir Path to local library.
   */
  private void readLocalLibrary(File fileDir) {

    LogUtils.debug(TAG, "++readLocalLibrary()");
    String resourcePath = BaseActivity.DEFAULT_LIBRARY_FILE;
    File file = new File(fileDir, resourcePath);
    LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
    if (file.exists() && file.canRead()) {
      for (ComicSeries series : mComicSeries.values()) { // reset comic books in each series
        series.ComicBooks = new ArrayList<>();
      }

      try (Reader reader = new FileReader(file.getAbsolutePath())) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<ComicBook>>() { }.getType();
        List<ComicBook> comics = gson.fromJson(reader, collectionType);
        for (ComicBook comic : comics) {
          if (comic.isValid()) {
            ComicSeries series = mComicSeries.get(comic.getProductId());
            if (series != null) {
              series.ComicBooks.add(comic);
            } else {
              LogUtils.warn(TAG, "Skipping %s; series %s not found.", comic.toString(), comic.getProductId());
            }
          }
        }
      } catch (Exception e) {
        LogUtils.warn(TAG, "Failed reading local library: %s", e.getMessage());
        Crashlytics.logException(e);
      }
    } else {
      LogUtils.debug(TAG, "%s does not exist yet.", resourcePath);
    }
  }

  private void replaceFragment(Fragment fragment) {

    LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.main_fragment_container, fragment);
    if (fragment.getClass().getName().equals(ComicSeriesListFragment.class.getName())) {
      fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    fragmentTransaction.addToBackStack(fragment.getClass().getName());
    LogUtils.debug(TAG, "Back stack count: %d", fragmentManager.getBackStackEntryCount());
    fragmentTransaction.commitAllowingStateLoss();
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

  private void syncComicBooks() {

    LogUtils.debug(TAG, "++syncComicBooks()");
    Intent intent = new Intent(this, SyncActivity.class);
    intent.putExtra(BaseActivity.ARG_USER, mUser);
    startActivityForResult(intent, BaseActivity.REQUEST_SYNC);
  }
}
