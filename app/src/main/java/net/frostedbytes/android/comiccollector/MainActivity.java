package net.frostedbytes.android.comiccollector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import android.widget.TextView;
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
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.common.SortUtils;
import net.frostedbytes.android.comiccollector.common.WriteToLocalLibraryTask;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
import net.frostedbytes.android.comiccollector.fragments.QueryFragment;
import net.frostedbytes.android.comiccollector.fragments.ScanResultsFragment;
import net.frostedbytes.android.comiccollector.models.ComicBook;
import net.frostedbytes.android.comiccollector.models.ComicSeries;
import net.frostedbytes.android.comiccollector.models.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class MainActivity extends AppCompatActivity implements
    ComicBookFragment.OnComicBookListener,
    ComicBookListFragment.OnComicBookListListener,
    QueryFragment.OnQueryListener,
    ScanResultsFragment.OnScanResultsListener {

    private static final String TAG = BASE_TAG + MainActivity.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;

    static final int CAMERA_PERMISSIONS_REQUEST = 11;
    static final int WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 12;

    private ProgressBar mProgressBar;
    private Snackbar mSnackbar;

    private ArrayList<ComicBook> mComicBooks;
    private Map<String, ComicSeries> mComicSeries;
    private File mCurrentImageFile;
    private Bitmap mImageBitmap;
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
        mProgressBar.setIndeterminate(true);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
            if (fragment != null) {
                updateTitle(fragment);
            }
        });

        mUser = new User();
        mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
        mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
        mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);

        // get comic series from firestore
        getRemoteData();

        // get user's permissions
        getUserPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        LogUtils.debug(TAG, "++onOptionsItemSelected(MenuItem)");
        switch (item.getItemId()) {
            case R.id.action_home:
                replaceFragment(ComicBookListFragment.newInstance(mComicBooks));
                break;
            case R.id.action_add:
                replaceFragment(QueryFragment.newInstance());
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
//                    Crashlytics.logException(e);
                }
            } else {
                try {
                    mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(mCurrentImageFile));
                } catch (FileNotFoundException e) {
//                    Crashlytics.logException(e);
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
                    readLocalLibrary();
                } else {
                    LogUtils.debug(TAG, "WRITE_EXTERNAL_STORAGE permission denied.");
                }

                break;
            case CAMERA_PERMISSIONS_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtils.debug(TAG, "CAMERA_PERMISSIONS_REQUEST permission granted.");
                    takePictureIntent();
                } else {
                    LogUtils.debug(TAG, "CAMERA_PERMISSIONS_REQUEST permission denied.");
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
            replaceFragment(ComicBookListFragment.newInstance(mComicBooks));
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
                if (comicBook.getUniqueId().equals(updatedComicBook.getUniqueId())) {
                    updatedComicBookList.add(updatedComicBook);
                } else {
                    updatedComicBookList.add(comicBook);
                }
            }

            new WriteToLocalLibraryTask(this, updatedComicBookList).execute();
            replaceFragment(ComicBookListFragment.newInstance(updatedComicBookList));
        }
    }

    @Override
    public void onComicListAddBook() {

        LogUtils.debug(TAG, "++onComicListAddBook()");
        replaceFragment(QueryFragment.newInstance());
    }

    @Override
    public void onComicListItemSelected(ComicBook comicBook) {

        LogUtils.debug(TAG, "++onComicListItemSelected(%s)", comicBook.toString());
        replaceFragment(ComicBookFragment.newInstance(mUser.Id, comicBook));
    }

    @Override
    public void onComicListPopulated(int size) {

        LogUtils.debug(TAG, "++onComicListPopulated(%d)", size);
        mProgressBar.setIndeterminate(false);
        if (size == 0) {
            mSnackbar = Snackbar.make(
                findViewById(R.id.main_fragment_container),
                getString(R.string.err_no_data),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(
                    getString(R.string.add),
                    view -> replaceFragment(QueryFragment.newInstance()));
            mSnackbar.show();
        }
    }

    @Override
    public void onComicListSynchronize() {

        LogUtils.debug(TAG, "++onComicListSynchronize()");
        readServerLibrary();
    }

    @Override
    public void onQueryActionComplete(String message) {

        LogUtils.debug(TAG, "++onQueryActionComplete(%s)", message);
        mProgressBar.setIndeterminate(false);
        if (!message.isEmpty()) {
            showDismissableSnackbar(message);
        }
    }

    @Override
    public void onQueryShowManualDialog() {

        LogUtils.debug(TAG, "++onQueryShowManualDialog()");
        mProgressBar.setIndeterminate(true);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_search_manual, null);
        EditText seriesText = promptView.findViewById(R.id.manual_dialog_edit_series);
        EditText issueText = promptView.findViewById(R.id.manual_dialog_edit_issue);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton(R.string.ok, (dialog, id) -> {

                ComicBook comicBook = new ComicBook();
                comicBook.SeriesCode = seriesText.getText().toString();
                comicBook.IssueCode = issueText.getText().toString();
                if (!comicBook.SeriesCode.equals(BaseActivity.DEFAULT_SERIES_CODE) &&
                    comicBook.SeriesCode.length() == BaseActivity.DEFAULT_SERIES_CODE.length()) {
                    if (!comicBook.IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) &&
                        comicBook.IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length()) {
                        queryInUserComicBooks(comicBook);
                    } else {
                        showDismissableSnackbar(getString(R.string.err_invalid_issue_code));
                    }
                } else {
                    showDismissableSnackbar(getString(R.string.err_invalid_series_code));
                }
            })
            .setNegativeButton(R.string.cancel, (dialog, id) -> {
                mProgressBar.setIndeterminate(false);
                dialog.cancel();
            });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    public void onQueryTakePicture() {

        LogUtils.debug(TAG, "++onQueryTakePicture()");
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            checkDevicePermission(Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST);
        } else {
            showDismissableSnackbar(getString(R.string.err_no_camera_detected));
        }
    }

    @Override
    public void onScanResultsPopulated(int size) {

        LogUtils.debug(TAG, "++onScanResultsPopulated(%d)", size);
        mProgressBar.setIndeterminate(false);
    }

    @Override
    public void onScanResultsItemSelected(String searchText) {

        LogUtils.debug(TAG, "++onScanResultsItemSelected(%s)", searchText);
//        ComicBook comicBook = new ComicBook();
//        comicBook.Title = searchText;
//        queryInUserComicBooks(comicBook);
    }

    /*
        Public Method(s)
     */
    public void writeComplete(ArrayList<ComicBook> comicBooks) {

        LogUtils.debug(TAG, "++writeComplete(%d)", comicBooks.size());
        mProgressBar.setIndeterminate(false);
        mComicBooks = comicBooks;
        replaceFragment(ComicBookListFragment.newInstance(mComicBooks));
    }

    /*
        Private Method(s)
     */
    private void addNewComicSeries(ComicBook comicBook) {

        LogUtils.debug(TAG, "++addNewComicSeries(%s)", comicBook.toString());
        ComicBook updated = new ComicBook(comicBook);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_comic_series, null);

        TextView codeText = promptView.findViewById(R.id.comic_series_edit_code);
        codeText.setText(comicBook.SeriesCode);

        EditText seriesEdit = promptView.findViewById(R.id.comic_series_edit_name);
        EditText volumeEdit = promptView.findViewById(R.id.comic_series_edit_volume);
        EditText publisherEdit = promptView.findViewById(R.id.comic_series_edit_publisher);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton(R.string.ok, (dialog, id) -> {
                ComicSeries newSeries = new ComicSeries();
                newSeries.Code = comicBook.SeriesCode;
                newSeries.Name = seriesEdit.getText().toString();
                newSeries.Publisher = publisherEdit.getText().toString();
                newSeries.Volume = Integer.parseInt(volumeEdit.getText().toString());
                String queryPath = PathUtils.combine(ComicSeries.ROOT, newSeries.Code);
                FirebaseFirestore.getInstance().document(queryPath).set(newSeries, SetOptions.merge())
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            mComicSeries.put(newSeries.Code, newSeries);
                            updated.SeriesName = newSeries.Name;
                            updated.Publisher = newSeries.Publisher;
                            updated.Volume = newSeries.Volume;
                            replaceFragment(ComicBookFragment.newInstance(mUser.Id, updated));
                        } else {
                            showDismissableSnackbar(getString(R.string.err_add_comic_series));
                        }
                    });
            })
            .setNegativeButton(R.string.cancel, (dialog, id) -> {
                // TODO: figure out where this path leads in overall execution
                mProgressBar.setIndeterminate(false);
                dialog.cancel();
            });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
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
                    readLocalLibrary();
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

    private void getIssueCodeFromUser(ComicBook comic) {

        LogUtils.debug(TAG, "++getIssueCodeFromUser(%s)", comic);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_issue_code, null);
        ImageView imageView = promptView.findViewById(R.id.issue_code_image_code);
        EditText seriesText = promptView.findViewById(R.id.issue_code_edit_series_code);
        seriesText.setText(comic.SeriesCode);
        EditText issueText = promptView.findViewById(R.id.issue_code_edit_issue_code);
        BitmapDrawable bmd = new BitmapDrawable(this.getResources(), mImageBitmap);
        imageView.setImageDrawable(bmd);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton(R.string.ok, (dialog, id) -> {
                // TODO: validate issue code
                comic.IssueCode = issueText.getText().toString();
                queryInUserComicBooks(comic);
            })
            .setNegativeButton(R.string.cancel, (dialog, id) -> {
                // TODO: figure out where this path leads in overall execution
                mProgressBar.setIndeterminate(false);
                dialog.cancel();
            });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void getRemoteData() {

        LogUtils.debug(TAG, "++getRemoteData()");
        mComicSeries = new HashMap<>();
        FirebaseFirestore.getInstance().collection(ComicSeries.ROOT).get().addOnCompleteListener(task -> {

                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot snapshot : task.getResult()) {
                        ComicSeries series = snapshot.toObject(ComicSeries.class);
                        if (series != null) {
                            series.Code = snapshot.getId();
                            mComicSeries.put(series.Code, series);
                        }
                    }
                } else {
                    parseComicSeriesAssetFile();
                }
            });
    }

    private void getUserPermissions() {

        LogUtils.debug(TAG, "++getUserPermissions()");
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id);
        FirebaseFirestore.getInstance().document(queryPath).get().addOnCompleteListener(this, task -> {

            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        mUser = user;
                        mUser.Id = snapshot.getId();
                    }
                }
            }

            if (!mUser.Id.isEmpty() && !mUser.Id.equals(BaseActivity.DEFAULT_USER_ID)) {
                checkDevicePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
            } else {
                showDismissableSnackbar(getString(R.string.err_unknown_user));
            }
        });
    }

    private void parseComicSeriesAssetFile() {

        LogUtils.debug(TAG, "++parseComicSeriesAssetFile()");
        mComicSeries = new HashMap<>();
        String parsableString;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getAssets().open("ComicSeries.txt"))));
            while ((parsableString = reader.readLine()) != null) { //process line
                if (parsableString.startsWith("--")) { // comment line; ignore
                    continue;
                }

                List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
                ComicSeries comicSeries = new ComicSeries();
                comicSeries.Code = elements.remove(0);
                comicSeries.Name = elements.remove(0);
                comicSeries.Volume = Integer.parseInt(elements.remove(0));
                comicSeries.Publisher = elements.remove(0);

                mComicSeries.put(comicSeries.Code, comicSeries);
            }
        } catch (IOException e) {
            showDismissableSnackbar(getString(R.string.err_comic_series_data_load_failed));
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    String errorMessage = getString(R.string.err_comic_series_data_cleanup_failed);
                    LogUtils.error(TAG, errorMessage);
                }
            }
        }
    }

    private void queryInUserComicBooks(ComicBook comicBook) {

        LogUtils.debug(TAG, "++queryInUserComicBooks(%s)", comicBook.toString());
        ComicBook foundBook = null;
        if (mComicBooks != null) {
            for (ComicBook comic : mComicBooks) {
                if (comic.getUniqueId().equals(comicBook.getUniqueId())) {
                    foundBook = comic;
                    break;
                }
            }
        }

        mProgressBar.setIndeterminate(false);
        if (foundBook != null) {
            replaceFragment(ComicBookFragment.newInstance(mUser.Id, foundBook));
        } else { // look up series info
            ComicSeries series = mComicSeries.get(comicBook.SeriesCode);
            if (series == null) {
                addNewComicSeries(comicBook);
            } else {
                comicBook.Publisher = series.Publisher;
                comicBook.SeriesName = series.Name;
                comicBook.Volume = series.Volume;
                replaceFragment(ComicBookFragment.newInstance(mUser.Id, comicBook));
            }
        }
    }

    private void readLocalLibrary() {

        LogUtils.debug(TAG, "++readLocalLibrary()");
        String parsableString;
        String resourcePath = BaseActivity.DEFAULT_LIBRARY_FILE;
        File file = new File(getFilesDir(), resourcePath);
        LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
        mComicBooks = new ArrayList<>();
        try {
            if (file.exists() && file.canRead()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                while ((parsableString = bufferedReader.readLine()) != null) { //process line
                    if (parsableString.startsWith("--")) { // comment line; ignore
                        continue;
                    }

                    List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
                    if (elements.size() != ComicBook.SCHEMA_FIELDS) {
                        LogUtils.debug(
                            TAG,
                            "Local library schema mismatch. Got: %d Expected: %d",
                            elements.size(),
                            ComicBook.SCHEMA_FIELDS);
                        continue;
                    }

                    ComicBook comicBook = new ComicBook();
                    comicBook.SeriesCode = elements.remove(0);
                    comicBook.IssueCode = elements.remove(0);
                    comicBook.Title = elements.remove(0);
                    comicBook.OwnedState = Boolean.parseBoolean(elements.remove(0));
                    comicBook.AddedDate = Long.parseLong(elements.remove(0));
                    comicBook.IssueCode = elements.remove(0);
                    comicBook.PublishedDate = Long.parseLong(elements.remove(0));
                    comicBook.UpdatedDate = Long.parseLong(elements.remove(0));

                    // attempt to locate this book in existing list
                    boolean comicFound = false;
                    for (ComicBook comic : mComicBooks) {
                        if (comic.getUniqueId().equals(comicBook.getUniqueId())) {
                            comicFound = true;
                            break;
                        }
                    }

                    if (!comicFound) {
                        mComicBooks.add(comicBook);
                        LogUtils.debug(TAG, "Adding %s to collection.", comicBook.toString());
                    }
                }
            } else {
                LogUtils.debug(TAG, "%s does not exist yet.", resourcePath);
            }
        } catch (Exception e) {
            LogUtils.warn(TAG, "Exception when reading local library data.");
//            Crashlytics.logException(e);
            mProgressBar.setIndeterminate(false);
        } finally {
            if (mComicBooks == null || mComicBooks.size() == 0) {
                readServerLibrary(); // attempt to get user's book library from cloud
            } else {
                mComicBooks.sort(new SortUtils.ByBookName());
                mProgressBar.setIndeterminate(false);
                replaceFragment(ComicBookListFragment.newInstance(mComicBooks));
            }
        }
    }

    private void readServerLibrary() {

        LogUtils.debug(TAG, "++readServerLibrary()");
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id, ComicBook.ROOT);
        LogUtils.debug(TAG, "QueryPath: %s", queryPath);
        mComicBooks = new ArrayList<>();
        FirebaseFirestore.getInstance().collection(queryPath).get().addOnCompleteListener(this, task -> {

            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                    ComicBook comicBook = document.toObject(ComicBook.class);
                    if (comicBook != null) {
                        String[] segments = document.getId().split("-");
                        comicBook.SeriesCode = segments[0];
                        comicBook.IssueCode = segments[1];
                        mComicBooks.add(comicBook);
                    } else {
                        LogUtils.warn(TAG, "Unable to convert user book: %s", queryPath);
                    }
                }

                mComicBooks.sort(new SortUtils.ByBookName());
                new WriteToLocalLibraryTask(this, mComicBooks).execute();
            } else {
                LogUtils.debug(TAG, "Could not get user book list: %s", queryPath);
                onComicListPopulated(0);
            }
        });
    }

    private void replaceFragment(Fragment fragment) {

        LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment_container, fragment);
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

    private void scanImageForText() {

        LogUtils.debug(TAG, "++scanImageForText()");
        if (mImageBitmap != null) {
            if (mUser.IsGeek) {
                LayoutInflater layoutInflater = LayoutInflater.from(this);
                View promptView = layoutInflater.inflate(R.layout.dialog_debug_image, null);
                ImageView imageView = promptView.findViewById(R.id.debug_dialog_image);
                BitmapDrawable bmd = new BitmapDrawable(this.getResources(), mImageBitmap);
                imageView.setImageDrawable(bmd);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setView(promptView);
                alertDialogBuilder.setCancelable(false)
                    .setPositiveButton(R.string.ok, (dialog, id) -> useFirebaseTextScanning())
                    .setNegativeButton(R.string.cancel, (dialog, id) -> {
                        mProgressBar.setIndeterminate(false);
                        dialog.cancel();
                    });

                AlertDialog alert = alertDialogBuilder.create();
                alert.show();
            } else {
                useFirebaseTextScanning();
            }
        } else {
            LogUtils.warn(TAG, getString(R.string.err_image_not_loaded));
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

    private void takePictureIntent() {

        LogUtils.debug(TAG, "++takePictureIntent()");
        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.dismiss();
        }

        deleteImageFile();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                mCurrentImageFile = createImageFile();
            } catch (IOException e) {
//                Crashlytics.logException(e);
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

    private void updateTitle(Fragment fragment) {

        LogUtils.debug(TAG, "++updateTitle(%s)", fragment.getClass().getName());
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(ComicBookListFragment.class.getName())) {
            setTitle(getString(R.string.comic_list));
        } else if (fragmentClassName.equals(QueryFragment.class.getName())) {
            setTitle(getString(R.string.query_for_comic_book));
        } else if (fragmentClassName.equals(ComicBookFragment.class.getName())) {
            setTitle(getString(R.string.comic_book));
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
                            if (barcodeValue != null && !barcodeValue.equals(BaseActivity.DEFAULT_SERIES_CODE)) {
                                comic.SeriesCode = barcodeValue;
                            }
                        } else {
                            LogUtils.warn(
                                TAG,
                                "Unexpected bar code: %s (%d)",
                                barcode.getDisplayValue(),
                                barcode.getValueType());
                        }
                    }

                    if (!comic.SeriesCode.isEmpty() && !comic.SeriesCode.equals(BaseActivity.DEFAULT_SERIES_CODE)) {
                        getIssueCodeFromUser(comic);
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
                        scanImageForText();
                    }
                } else {
                    showDismissableSnackbar(getString(R.string.err_bar_code_task));
                    replaceFragment(ComicBookListFragment.newInstance(mComicBooks));
                }
            });
    }

    private void useFirebaseTextScanning() {

        LogUtils.debug(TAG, "++useFirebaseTextScanning()");
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        com.google.android.gms.tasks.Task<FirebaseVisionText> result = detector.processImage(image).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<String> blocks = new ArrayList<>();
                for (FirebaseVisionText.TextBlock textBlock : task.getResult().getTextBlocks()) {
                    String block = textBlock.getText().replace("\n", " ").replace("\r", " ");
                    blocks.add(block);
                }

                if (blocks.size() > 0) {
                    replaceFragment(ScanResultsFragment.newInstance(blocks));
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
                    scanImageForText();
                } else {
                    showDismissableSnackbar(getString(R.string.err_no_bar_code_or_text));
                    replaceFragment(ComicBookListFragment.newInstance(mComicBooks));
                }
            } else {
                showDismissableSnackbar(getString(R.string.err_text_detection_task));
                replaceFragment(ComicBookListFragment.newInstance(mComicBooks));
            }
        });
    }
}
