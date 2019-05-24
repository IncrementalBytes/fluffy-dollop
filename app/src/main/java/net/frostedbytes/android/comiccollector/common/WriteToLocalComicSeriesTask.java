package net.frostedbytes.android.comiccollector.common;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.AsyncTask;
import com.crashlytics.android.Crashlytics;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.MainActivity;
import net.frostedbytes.android.comiccollector.models.ComicSeries;

public class WriteToLocalComicSeriesTask extends AsyncTask<Void, Void, Collection<ComicSeries>> {

  private static final String TAG = BASE_TAG + WriteToLocalComicSeriesTask.class.getSimpleName();

  private final WeakReference<MainActivity> mFragmentWeakReference;
  private final Collection<ComicSeries> mComicSeries;

  public WriteToLocalComicSeriesTask(MainActivity context, Collection<ComicSeries> comicSeries) {

    mFragmentWeakReference = new WeakReference<>(context);
    mComicSeries = comicSeries;
  }

  protected ArrayList<ComicSeries> doInBackground(Void... params) {

    ArrayList<ComicSeries> seriesWritten = new ArrayList<>();
    FileOutputStream outputStream;
    try {
      outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
        BaseActivity.DEFAULT_COMIC_SEREIS_FILE,
        Context.MODE_PRIVATE);
      for (ComicSeries comicSeries : mComicSeries) {
        outputStream.write(comicSeries.writeLine().getBytes());
        seriesWritten.add(comicSeries);
      }
    } catch (Exception e) {
      LogUtils.warn(TAG, "Exception when writing local library.");
      Crashlytics.logException(e);
    }

    return seriesWritten;
  }

  protected void onPostExecute(ArrayList<ComicSeries> comicSeries) {

    LogUtils.debug(TAG, "++onPostExecute(%d)", comicSeries.size());
    MainActivity activity = mFragmentWeakReference.get();
    if (activity == null) {
      LogUtils.error(TAG, "Activity is null.");
      return;
    }

    activity.writeComicSeriesComplete(comicSeries);
  }
}
