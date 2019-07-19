package net.frostedbytes.android.comiccollector;

import android.Manifest;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import java.util.Locale;
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

  private ArrayList<ComicBook> mComicBooks;
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
    if (User.isValid(mUser)) {
      mProgressBar.setIndeterminate(true);
      readServerComicPublishers();
    } else {
      // TODO: add process for unknown user
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
          checkDevicePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS);
        } else {
          showDismissableSnackbar(getString(R.string.err_unknown_user));
        }

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
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    LogUtils.debug(TAG, "++onSaveInstanceState(Bundle)");
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    LogUtils.debug(TAG, "++onActivityResult(%d, %d, Intent)", requestCode, resultCode);
    if (resultCode != RESULT_OK) {
      // TODO: not good
      return;
    }

    String message;
    switch (requestCode) {
      case BaseActivity.REQUEST_COMIC_ADD:
        message = data.getStringExtra(BaseActivity.ARG_MESSAGE);
        if (message != null && message.length() > 0) {
          showDismissableSnackbar(message);
        }

        ComicSeries series = data.getParcelableExtra(BaseActivity.ARG_COMIC_SERIES);
        if (series != null) {
          if (!mComicSeries.containsKey(series.getProductId())) {
            mComicSeries.put(series.getProductId(), series);
            new WriteToLocalComicSeriesTask(this, new ArrayList<>(mComicSeries.values()));
          }
        }

        ComicBook book = data.getParcelableExtra(BaseActivity.ARG_COMIC_BOOK);
        if (book != null && book.isValid()) {
          if (series == null) {
            series = mComicSeries.get(book.getProductId());
          }

          ComicPublisher comicPublisher = mPublishers.get(book.PublisherId);
          replaceFragment(ComicBookFragment.newInstance(mUser.Id, book, comicPublisher, series));
        }
        break;
      default:
        message = String.format(Locale.US, "Unknown request code: %d", requestCode);
        showDismissableSnackbar(message);
        LogUtils.warn(TAG, message);
        break;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    LogUtils.debug(TAG, "++onRequestPermissionsResult(int, String[], int[])");
    switch (requestCode) {
      case BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS:
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
      LogUtils.debug(TAG, "++onComicBookAddedToLibrary(null)");
      showDismissableSnackbar(getString(R.string.err_add_comic_book));
    } else {
      LogUtils.debug(TAG, "++onComicBookAddedToLibrary(%s)", comicBook.toString());
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
      LogUtils.debug(TAG, "++onComicBookRemoved(null)");
      showDismissableSnackbar(getString(R.string.err_remove_comic_book));
    } else {
      LogUtils.debug(TAG, "++onComicBookRemoved(%s)", comicBook.toString());
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
      LogUtils.debug(TAG, "++onComicBookUpdated(null)");
      showDismissableSnackbar(getString(R.string.err_update_comic_book));
    } else {
      LogUtils.debug(TAG, "++onComicBookUpdated(%s)", updatedComicBook.toString());
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
    addComicBook();
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
  public void onComicListSynchronize() {

    LogUtils.debug(TAG, "++onComicListSynchronize()");
    mProgressBar.setIndeterminate(true);
    if (mMainToolbar != null && mMainToolbar.getMenu() != null) {
      MenuItem item = mMainToolbar.getMenu().findItem(R.id.action_add);
      if (item != null) {
        item.setEnabled(false);
      }

      item = mMainToolbar.getMenu().findItem(R.id.action_home);
      if (item != null) {
        item.setEnabled(false);
      }
    }

    readServerComicPublishers();
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
      if (!mComicSeries.containsKey(series.getProductId())) {
        mComicSeries.put(series.getProductId(), series);
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

  private void addComicBook() {

    LogUtils.debug(TAG, "++addComicBook()");
    Intent intent = new Intent(this, AddActivity.class);
    intent.putExtra(BaseActivity.ARG_USER, mUser);
    intent.putParcelableArrayListExtra(BaseActivity.ARG_COMIC_PUBLISHERS, new ArrayList<>(mPublishers.values()));
    intent.putParcelableArrayListExtra(BaseActivity.ARG_COMIC_SERIES, new ArrayList<>(mComicSeries.values()));
    intent.putParcelableArrayListExtra(BaseActivity.ARG_COMIC_BOOK_LIST, mComicBooks);
    startActivityForResult(intent, BaseActivity.REQUEST_COMIC_ADD);
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
        case BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS:
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
      }
    }
  }

  private void readServerComicPublishers() {

    LogUtils.debug(TAG, "++readServerComicPublishers()");
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
            checkDevicePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS);
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
        checkDevicePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS);
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
}
