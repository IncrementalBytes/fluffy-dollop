package net.frostedbytes.android.comiccollector;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
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
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.WriteToLocalLibraryTask;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
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
  UserPreferenceFragment.OnPreferencesListener {

  private static final String TAG = BASE_TAG + "MainActivity";

  private Toolbar mMainToolbar;
  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

  private ListenerRegistration mComicPublishersRegistration;
  private ListenerRegistration mComicSeriesRegistration;
  private FirebaseFirestore mFirestore;

  private HashMap<String, ComicBook> mComicBooks;
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

    mComicBooks = new HashMap<>();
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
      readServerComicPublishers();
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
    mComicBooks = null;
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
    String message;
    switch (requestCode) {
      case BaseActivity.REQUEST_COMIC_ADD:
        message = data.getStringExtra(BaseActivity.ARG_MESSAGE);
        if (resultCode != RESULT_OK) {
          if (message != null && message.length() > 0) {
            showDismissableSnackbar(message);
          } else {
            LogUtils.error(TAG, "Activity result failed for an unknown reason.");
          }
        } else {
          ComicSeries series = data.getParcelableExtra(BaseActivity.ARG_COMIC_SERIES);
          if (series != null) {
            if (!mComicSeries.containsKey(series.getProductId())) {
              mComicSeries.put(series.getProductId(), series);
            }
          }

          ComicBook book = data.getParcelableExtra(BaseActivity.ARG_COMIC_BOOK);
          if (book != null && book.isValid()) {
            ComicPublisher comicPublisher = mPublishers.get(book.PublisherId);
            ComicSeries comicSeries = mComicSeries.get(book.getProductId());
            if (comicPublisher != null && comicSeries != null) {
              replaceFragment(ComicBookFragment.newInstance(book, comicPublisher, comicSeries, true));
            } else {
              LogUtils.warn(TAG, "Publisher and/or Series data is unexpected.");
            }
          }

          if (message != null && message.length() > 0) {
            showDismissableSnackbar(message);
          }
        }
        break;
      case BaseActivity.REQUEST_SYNC:
        message = data.getStringExtra(BaseActivity.ARG_MESSAGE);
        if (resultCode != RESULT_OK) {
          if (message != null && message.length() > 0) {
            showDismissableSnackbar(message);
          } else {
            LogUtils.error(TAG, "Activity result failed for an unknown reason.");
          }
        } else {
          boolean reload = data.getBooleanExtra(BaseActivity.ARG_RELOAD, false);
          if (reload) {
            mComicBooks = ComicBook.readLocalLibrary(getFilesDir());
            replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
          }

          if (message != null && message.length() > 0) {
            showDismissableSnackbar(message);
          }
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
      if (!mComicBooks.containsKey(comicBook.getFullId())) {
        mComicBooks.put(comicBook.getFullId(), comicBook);
      }

      new WriteToLocalLibraryTask(this, new ArrayList<>(mComicBooks.values())).execute();
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
      mComicBooks.remove(comicBook.getFullId());
      new WriteToLocalLibraryTask(this, new ArrayList<>(mComicBooks.values())).execute();
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

  /*
      Public Method(s)
   */
  public void writeLibraryComplete(HashMap<String, ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++writeLibraryComplete(%d)", comicBooks.size());
    mProgressBar.setIndeterminate(false);
    mComicBooks = comicBooks;
    replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
  }

  /*
      Private Method(s)
   */

  private void addComicBook() {

    LogUtils.debug(TAG, "++addComicBook()");
    Intent intent = new Intent(this, AddActivity.class);
    intent.putExtra(BaseActivity.ARG_USER, mUser);
    intent.putParcelableArrayListExtra(BaseActivity.ARG_COMIC_PUBLISHERS, new ArrayList<>(mPublishers.values()));
    intent.putParcelableArrayListExtra(BaseActivity.ARG_COMIC_SERIES, new ArrayList<>(mComicSeries.values()));
    intent.putParcelableArrayListExtra(BaseActivity.ARG_COMIC_BOOK_LIST, new ArrayList<>(mComicBooks.values()));
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
      mComicBooks = ComicBook.readLocalLibrary(getFilesDir());
      replaceFragment(ComicBookListFragment.newInstance(mComicBooks, mPublishers, mComicSeries));
    }
  }

  private void readServerComicPublishers() {

    LogUtils.debug(TAG, "++readServerComicPublishers()");
    mPublishers = new HashMap<>();
    Trace comicPublisherTrace = FirebasePerformance.getInstance().newTrace("get_comic_publishers");
    comicPublisherTrace.start();
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
        ComicPublisher publisher = dc.getDocument().toObject(ComicPublisher.class);
        publisher.Id = dc.getDocument().getId();
        comicPublisherTrace.incrementMetric("comic_publisher_read", 1);
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

      comicPublisherTrace.stop();
      readServerComicSeries();
    });
  }

  private void readServerComicSeries() {

    LogUtils.debug(TAG, "++readServerComicSeries()");
    mComicSeries = new HashMap<>();
    Trace comicSeriesTrace = FirebasePerformance.getInstance().newTrace("get_comic_series");
    comicSeriesTrace.start();
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
        ComicSeries series = dc.getDocument().toObject(ComicSeries.class);
        series.parseProductCode(dc.getDocument().getId());
        switch (dc.getType()) {
          case ADDED:
            LogUtils.debug(TAG, "New series: %s", series.toString());
            if (!series.PublisherId.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID) ||
              !series.Id.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID)) {
              mComicSeries.put(series.getProductId(), series);
              comicSeriesTrace.incrementMetric("comic_series_read", 1);
            } else {
              comicSeriesTrace.incrementMetric("comic_series_bad_format", 1);
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

      // series data is loaded, proceed to loading comic library data
      checkForWritePermission();
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
    startActivityForResult(intent, BaseActivity.REQUEST_SYNC);  }
}
