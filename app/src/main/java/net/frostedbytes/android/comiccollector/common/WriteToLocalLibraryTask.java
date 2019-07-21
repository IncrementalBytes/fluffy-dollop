package net.frostedbytes.android.comiccollector.common;

import android.content.Context;
import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
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
      outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
        BaseActivity.DEFAULT_LIBRARY_FILE,
        Context.MODE_PRIVATE);
      for (ComicBook comicBook : mComicBooks) {
        String lineToWrite = comicBook.writeLine();
        LogUtils.debug(TAG, "Writing: %s", lineToWrite);
        outputStream.write(lineToWrite.getBytes());
        booksWritten.put(comicBook.getFullId(), comicBook);
      }
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
