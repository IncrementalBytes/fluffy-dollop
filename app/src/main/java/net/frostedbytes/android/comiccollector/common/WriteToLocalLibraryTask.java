package net.frostedbytes.android.comiccollector.common;

import android.content.Context;
import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.MainActivity;
import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import net.frostedbytes.android.comiccollector.models.ComicSeries;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class WriteToLocalLibraryTask extends AsyncTask<Void, Void, ArrayList<ComicBook>> {

  private static final String TAG = BASE_TAG + "WriteToLocalLibraryTask";

  private final WeakReference<MainActivity> mFragmentWeakReference;
  private final ArrayList<ComicSeries> mComicSeries;

  public WriteToLocalLibraryTask(MainActivity context, ArrayList<ComicSeries> comicSeries) {

    mFragmentWeakReference = new WeakReference<>(context);
    mComicSeries = comicSeries;
  }

  protected ArrayList<ComicBook> doInBackground(Void... params) {

    ArrayList<ComicBook> booksWritten = new ArrayList<>();
    FileOutputStream outputStream;
    try {
      outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
        BaseActivity.DEFAULT_LIBRARY_FILE,
        Context.MODE_PRIVATE);
      Gson gson = new Gson();
      Type collectionType = new TypeToken<ArrayList<ComicBook>>() {}.getType();
      for (ComicSeries series : mComicSeries) {
        booksWritten.addAll(series.ComicBooks);
      }

      outputStream.write(gson.toJson(booksWritten, collectionType).getBytes());
    } catch (Exception e) {
      LogUtils.warn(TAG, "Exception when writing local library.");
      Crashlytics.logException(e);
    }

    return booksWritten;
  }

  protected void onPostExecute(ArrayList<ComicBook> comicBooks) {

    LogUtils.debug(TAG, "++onPostExecute(%d)", comicBooks.size());
    MainActivity activity = mFragmentWeakReference.get();
    if (activity == null) {
      LogUtils.error(TAG, "Activity is null.");
      return;
    }

    activity.writeLibraryComplete(comicBooks);
  }
}
