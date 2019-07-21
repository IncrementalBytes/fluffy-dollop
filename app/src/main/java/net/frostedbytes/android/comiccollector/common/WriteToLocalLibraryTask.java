package net.frostedbytes.android.comiccollector.common;

import android.content.Context;
import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.MainActivity;
import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class WriteToLocalLibraryTask extends AsyncTask<Void, Void, HashMap<String, ComicBook>> {

  private static final String TAG = BASE_TAG + "WriteToLocalLibraryTask";

  private final WeakReference<MainActivity> mFragmentWeakReference;
  private final ArrayList<ComicBook> mComicBooks;

  public WriteToLocalLibraryTask(MainActivity context, ArrayList<ComicBook> comicBooks) {

    mFragmentWeakReference = new WeakReference<>(context);
    mComicBooks = comicBooks;
  }

  protected HashMap<String, ComicBook> doInBackground(Void... params) {

    HashMap<String, ComicBook> booksWritten = new HashMap<>();
    FileOutputStream outputStream;
    try {
      // for future processing consideration
      for (ComicBook comicBook : mComicBooks) {
        if (comicBook.isValid()) {
          booksWritten.put(comicBook.getFullId(), comicBook);
        }
      }

      outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
        BaseActivity.DEFAULT_LIBRARY_FILE,
        Context.MODE_PRIVATE);
      Gson gson = new Gson();
      Type collectionType = new TypeToken<ArrayList<ComicBook>>(){}.getType();
      outputStream.write(gson.toJson(mComicBooks, collectionType).getBytes());
    } catch (Exception e) {
      LogUtils.warn(TAG, "Exception when writing local library.");
      Crashlytics.logException(e);
    }

    return booksWritten;
  }

  protected void onPostExecute(HashMap<String, ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++onPostExecute(%d)", comicBooks.size());
    MainActivity activity = mFragmentWeakReference.get();
    if (activity == null) {
      LogUtils.error(TAG, "Activity is null.");
      return;
    }

    activity.writeLibraryComplete(comicBooks);
  }
}
