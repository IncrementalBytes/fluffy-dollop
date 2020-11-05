/*
 * Copyright 2020 Ryan Ward
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

package net.whollynugatory.android.comiccollector.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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
import net.whollynugatory.android.comiccollector.common.PathUtils;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.entity.UserEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.R;

public class SyncFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "SyncFragment";

  public interface OnSyncListener {

    void onSyncExport();
    void onSyncImport();
    void onSyncErrorStatus(String message);
  }

  private OnSyncListener mCallback;

  private CollectorViewModel mCollectorViewModel;

  private UserEntity mUser;

  public static SyncFragment newInstance(UserEntity user) {

    Log.d(TAG, "++newInstance(UserEntity)");
    SyncFragment fragment = new SyncFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_USER, user);
    fragment.setArguments(args);
    return fragment;
  }

  /*
  Fragment Override(s)
  */
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnSyncListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mUser = (UserEntity) arguments.getSerializable(BaseActivity.ARG_USER);
    } else {
      Log.e(TAG, "Arguments were null.");
    }

    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_sync, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Log.d(TAG, "++onViewCreated(View, Bundle)");
    CardView exportCard = view.findViewById(R.id.sync_card_export);
    CardView importCard = view.findViewById(R.id.sync_card_import);

    if (mUser != null && UserEntity.isValid(mUser)) {
      exportCard.setOnClickListener(v -> exportLibrary());
      importCard.setOnClickListener(v -> importLibrary());
    } else {
      mCallback.onSyncErrorStatus(getString(R.string.err_sync_unknown_user));
    }
  }

  private void exportLibrary() {

    Log.d(TAG, "++exportLibrary()");
    String remotePath = PathUtils.combine(UserEntity.ROOT, mUser.Id, BaseActivity.DEFAULT_LIBRARY_FILE);
    mCollectorViewModel.exportComics().observe(getViewLifecycleOwner(), comicEntityList -> {

      if (comicEntityList != null) {
        FileOutputStream outputStream;
        try {
          outputStream = getActivity().openFileOutput(BaseActivity.DEFAULT_EXPORT_FILE, Context.MODE_PRIVATE);
          Gson gson = new Gson();
          Type collectionType = new TypeToken<ArrayList<ComicBookEntity>>() {
          }.getType();
          ArrayList<ComicBookEntity> booksWritten = new ArrayList<>(comicEntityList);
          outputStream.write(gson.toJson(booksWritten, collectionType).getBytes());
          Log.d(TAG, "Comic books written: " + booksWritten.size());
        } catch (Exception e) {
          Log.w(TAG, "Exception when exporting local database.");
          FirebaseCrashlytics.getInstance().recordException(e);
        }
      }

      try { // look for file output
        InputStream stream = getActivity().openFileInput(BaseActivity.DEFAULT_EXPORT_FILE);
        UploadTask uploadTask = FirebaseStorage.getInstance().getReference().child(remotePath).putStream(stream);
        uploadTask.addOnCompleteListener(task -> {

          if (task.isSuccessful()) {
            if (task.getResult() != null) {
              mCallback.onSyncExport();
            } else {
              Log.w(TAG, "Storage task results were null; this is unexpected.");
              mCallback.onSyncErrorStatus(getString(R.string.err_storage_task_unexpected));
            }
          } else {
            if (task.getException() != null) {
              Log.e(TAG, "Could not export library.", task.getException());
            }
          }
        });
      } catch (FileNotFoundException fnfe) {
        Log.w(TAG, "Could not export library.", fnfe);
        FirebaseCrashlytics.getInstance().recordException(fnfe);
      } finally {
        File tempFile = new File(getActivity().getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
        if (tempFile.exists()) {
          if (tempFile.delete()) {
            Log.d(TAG, "Removed temporary local export file.");
          } else {
            Log.w(TAG, "Temporary file was not removed.");
          }
        }
      }
    });
  }

  private void importLibrary() {

    Log.d(TAG, "++importLibrary()");
    String remotePath = PathUtils.combine(UserEntity.ROOT, mUser.Id, BaseActivity.DEFAULT_LIBRARY_FILE);
    StorageReference storage = FirebaseStorage.getInstance().getReference().child(remotePath);

    File localFile = new File(getActivity().getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
    FirebaseStorage.getInstance().getReference().child(remotePath).getFile(localFile).addOnCompleteListener(task -> {

      if (task.isSuccessful() && task.getException() == null) {
        File file = new File(getActivity().getFilesDir(), BaseActivity.DEFAULT_EXPORT_FILE);
        Log.d(TAG, "Loading " + file.getAbsolutePath());
        if (file.exists() && file.canRead()) {
          try (Reader reader = new FileReader(file.getAbsolutePath())) {
            mCollectorViewModel.deleteAllComicBooks();
            Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<ComicBookEntity>>() {
            }.getType();
            List<ComicBookEntity> comics = gson.fromJson(reader, collectionType);
            List<ComicBookEntity> updatedComics = new ArrayList<>();
            for (ComicBookEntity comicBook : comics) {
              mCollectorViewModel.insertComicBook(comicBook);
            }

            mCallback.onSyncImport();
          } catch (Exception e) {
            Log.w(TAG, "Failed reading local library.", e);
            FirebaseCrashlytics.getInstance().recordException(e);
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
            mCallback.onSyncErrorStatus(getString(R.string.err_remote_library_not_found));
          } else {
            Log.e(TAG, "Could not import library.", task.getException());
            mCallback.onSyncErrorStatus(getString(R.string.err_import_task));
          }
        } else {
          mCallback.onSyncErrorStatus(getString(R.string.err_import_unknown));
        }
      }
    });
  }
}
