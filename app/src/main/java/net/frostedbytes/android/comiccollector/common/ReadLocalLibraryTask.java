package net.frostedbytes.android.comiccollector.common;

//import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;
//
//import android.os.AsyncTask;
//import com.crashlytics.android.Crashlytics;
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import java.io.File;
//import java.io.FileReader;
//import java.io.Reader;
//import java.lang.ref.WeakReference;
//import java.lang.reflect.Type;
//import java.util.ArrayList;
//import java.util.List;
//import net.frostedbytes.android.comiccollector.BaseActivity;
//import net.frostedbytes.android.comiccollector.MainActivity;
//import net.frostedbytes.android.comiccollector.models.ComicBook;
//
//public class ReadLocalLibraryTask extends AsyncTask<Void, Void, ArrayList<ComicBook>> {
//
//  private static final String TAG = BASE_TAG + "ReadLocalLibraryTask";
//
//  private WeakReference<MainActivity> mActivityWeakReference;
//  private File mFileDir;
//
//  public ReadLocalLibraryTask(MainActivity context, File fileDir) {
//
//    mActivityWeakReference = new WeakReference<>(context);
//    mFileDir = fileDir;
//  }
//
//  protected ArrayList<ComicBook> doInBackground(Void... params) {
//
//    ArrayList<ComicBook> comicBooks = new ArrayList<>();
//    String resourcePath = BaseActivity.DEFAULT_LIBRARY_FILE;
//    File file = new File(mFileDir, resourcePath);
//    LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
//    if (file.exists() && file.canRead()) {
//      try (Reader reader = new FileReader(file.getAbsolutePath())) {
//        Gson gson = new Gson();
//        Type collectionType = new TypeToken<ArrayList<ComicBook>>() { }.getType();
//        List<ComicBook> comics = gson.fromJson(reader, collectionType);
//        for (ComicBook comic : comics) {
//          comic.parseIssueCode(comic.IssueCode);
//          if (comic.isValid()) {
//            if (!comicBooks.contains(comic)) {
//              comicBooks.add(comic);
//            }
//          }
//        }
//      } catch (Exception e) {
//        LogUtils.warn(TAG, "Failed reading local library: %s", e.getMessage());
//        Crashlytics.logException(e);
//      }
//    } else {
//      LogUtils.debug(TAG, "%s does not exist yet.", resourcePath);
//    }
//
//    return comicBooks;
//  }
//
//  protected void onPostExecute(ArrayList<ComicBook> comicBooks) {
//
//    LogUtils.debug(TAG, "++onPostExecute(%d)", comicBooks.size());
//    MainActivity activity = mActivityWeakReference.get();
//    if (activity == null) {
//      LogUtils.error(TAG, "MainActivity is null or detached.");
////      return;
//    }
//
////    activity.retrieveLocalLibraryComplete(comicBooks);
//  }
//}
//
