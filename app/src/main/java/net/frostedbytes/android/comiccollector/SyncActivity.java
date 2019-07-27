package net.frostedbytes.android.comiccollector;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.common.PathUtils;
import net.frostedbytes.android.comiccollector.fragments.SyncFragment;
import net.frostedbytes.android.comiccollector.models.User;

public class SyncActivity extends BaseActivity implements
  SyncFragment.OnSyncListener {

  private static final String TAG = BASE_TAG + "SyncActivity";

  private StorageReference mStorage;

  private String mRemotePath;
  private User mUser;

  /*
    AppCompatActivity Override(s)
 */
  @Override
  public void onBackPressed() {

    LogUtils.debug(TAG, "++onBackPressed()");
    if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
      setResultAndFinish(RESULT_CANCELED, "");
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LogUtils.debug(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_sync);

    Toolbar mSyncToolbar = findViewById(R.id.sync_toolbar);
    setSupportActionBar(mSyncToolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.sync_fragment_container);
      if (fragment != null) {
        updateTitle(fragment);
      }
    });

    mUser = (User) getIntent().getSerializableExtra(BaseActivity.ARG_USER);

    if (User.isValid(mUser)) {
      mRemotePath = PathUtils.combine(User.ROOT, mUser.Id, BaseActivity.DEFAULT_LIBRARY_FILE);
      mStorage = FirebaseStorage.getInstance().getReference();
      replaceFragment(SyncFragment.newInstance(mUser));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    LogUtils.debug(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.menu_sync, menu);
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
    if (item.getItemId() == R.id.action_cancel) {
      setResultAndFinish(RESULT_CANCELED, "");
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (mStorage != null) {
      outState.putString("reference", mStorage.toString());
    }
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
      task.addOnSuccessListener(this, state -> setResultAndFinish(BaseActivity.RESULT_IMPORT_SUCCESS, ""));
    }
  }

  /*
    Private Method(s)
   */
  private void setResultAndFinish(int resultCode, String message) {

    LogUtils.debug(TAG, "++setResultAndFinish(%d, %s)", resultCode, message);
    Intent resultIntent = new Intent();
    resultIntent.putExtra(BaseActivity.ARG_MESSAGE, message);
    setResult(resultCode, resultIntent);
    finish();
  }

  /*
    Fragment Callback(s)
   */
  @Override
  public void onExport() {

    LogUtils.debug(TAG, "++onExport()");
    try {
      InputStream stream = getApplicationContext().openFileInput(BaseActivity.DEFAULT_LIBRARY_FILE);
      StorageReference libraryRef = mStorage.child(mRemotePath);
      UploadTask uploadTask = libraryRef.putStream(stream);
      uploadTask.addOnCompleteListener(task -> {

        if (task.isSuccessful()) {
          if (task.getResult() != null) {
            if (task.getResult().getMetadata() != null) {
              setResultAndFinish(BaseActivity.RESULT_EXPORT_SUCCESS, "");
            }
          } else {
            LogUtils.warn(TAG, "Storage task results were null; this is unexpected.");
            setResultAndFinish(BaseActivity.RESULT_SYNC_FAILED, getString(R.string.err_storage_task_unexpected));
          }
        } else {
          if (task.getException() != null) {
            LogUtils.error(TAG, "Could not export library: %s", task.getException().getMessage());
          }

          setResultAndFinish(BaseActivity.RESULT_SYNC_FAILED, getString(R.string.err_export_task));
        }
      });
    } catch (FileNotFoundException fe) {
      LogUtils.warn(TAG, fe.getMessage());
      setResultAndFinish(BaseActivity.RESULT_SYNC_FAILED, getString(R.string.err_export));
    }
  }

  @Override
  public void onImport() {

    LogUtils.debug(TAG, "++onImport()");
    StorageReference libraryRef = mStorage.child(mRemotePath);
    File localFile = new File(getFilesDir(), BaseActivity.DEFAULT_LIBRARY_FILE);
    libraryRef.getFile(localFile).addOnCompleteListener(task -> {

      if (task.isSuccessful() && task.getException() == null) {
        setResultAndFinish(BaseActivity.RESULT_IMPORT_SUCCESS, "");
      } else {
        if (task.getException() != null) {
          StorageException exception = (StorageException) task.getException();
          if (exception.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
            setResultAndFinish(BaseActivity.RESULT_SYNC_FAILED, getString(R.string.err_remote_library_not_found));
          } else {
            LogUtils.error(TAG, "Could not import library: %s", task.getException().getMessage());
            setResultAndFinish(BaseActivity.RESULT_SYNC_FAILED, getString(R.string.err_import_task));
          }
        } else {
          setResultAndFinish(BaseActivity.RESULT_SYNC_FAILED, "");
        }
      }
    });
  }

  @Override
  public void onSyncFail() {

    LogUtils.debug(TAG, "++onSyncFail()");
    String message = "Setting up synchronization failed; user unknown.";
    LogUtils.warn(TAG, message);
    setResultAndFinish(BaseActivity.RESULT_SYNC_FAILED, message);
  }

  /*
    Private Method(s)
   */
  private void replaceFragment(Fragment fragment) {

    LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.sync_fragment_container, fragment);
    if (fragment.getClass().getName().equals(SyncFragment.class.getName())) {
      fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    fragmentTransaction.addToBackStack(fragment.getClass().getName());
    LogUtils.debug(TAG, "Back stack count: %d", fragmentManager.getBackStackEntryCount());
    fragmentTransaction.commitAllowingStateLoss();
  }
}
