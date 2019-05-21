package net.frostedbytes.android.comiccollector.common;

import android.content.Context;
import android.os.AsyncTask;

import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.MainActivity;
import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

public class WriteToLocalLibraryTask extends AsyncTask<Void, Void, ArrayList<ComicBook>> {

    private static final String TAG = BASE_TAG + WriteToLocalLibraryTask.class.getSimpleName();

    private final WeakReference<MainActivity> mFragmentWeakReference;
    private final ArrayList<ComicBook> mComicBooks;

    public WriteToLocalLibraryTask(MainActivity context, ArrayList<ComicBook> comicBooks) {

        mFragmentWeakReference = new WeakReference<>(context);
        mComicBooks = comicBooks;
    }

    protected ArrayList<ComicBook> doInBackground(Void... params) {

        ArrayList<ComicBook> booksWritten = new ArrayList<>();
        FileOutputStream outputStream;
        try {
            outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
                BaseActivity.DEFAULT_LIBRARY_FILE,
                Context.MODE_PRIVATE);
            for (ComicBook comicBook : mComicBooks) {
                String lineContents = String.format(
                    Locale.US,
                    "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s\r\n",
                    comicBook.SeriesCode,
                    comicBook.SeriesName,
                    comicBook.Volume,
                    comicBook.IssueCode,
                    comicBook.Title,
                    String.valueOf(comicBook.OwnedState),
                    String.valueOf(comicBook.AddedDate),
                    comicBook.Publisher,
                    String.valueOf(comicBook.PublishedDate),
                    String.valueOf(comicBook.UpdatedDate));
                outputStream.write(lineContents.getBytes());
                booksWritten.add(comicBook);
            }
        } catch (Exception e) {
            LogUtils.warn(TAG, "Exception when writing local library.");
//            Crashlytics.logException(e);
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

        activity.writeComplete(comicBooks);
    }
}
