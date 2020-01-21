/*
 * Copyright 2019 Ryan Ward
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

package net.whollynugatory.android.comiccollector.db;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.db.dao.ComicBookDao;
import net.whollynugatory.android.comiccollector.db.dao.PublisherDao;
import net.whollynugatory.android.comiccollector.db.dao.SeriesDao;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.entity.RemoteData;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;

@Database(
  entities = {ComicBookEntity.class, PublisherEntity.class, SeriesEntity.class},
  version = 1,
  exportSchema = false)
public abstract class CollectorDatabase extends RoomDatabase {

  private static final String TAG = BaseActivity.BASE_TAG + "CollectorDatabase";

  public abstract ComicBookDao comicBookDao();

  public abstract PublisherDao publisherDao();

  public abstract SeriesDao seriesDao();

  private static volatile CollectorDatabase INSTANCE;
  private static volatile File sData;
  private static volatile File sLibrary;
  private static final int NUMBER_OF_THREADS = 4;

  public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

  public static CollectorDatabase getInstance(final Context context) {

    if (INSTANCE == null) {
      synchronized (CollectorDatabase.class) {
        if (INSTANCE == null) {
          sLibrary = new File(context.getFilesDir(), BaseActivity.DEFAULT_LIBRARY_FILE);
          sData = new File(context.getCacheDir(), BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);

          File remoteData = new File(context.getFilesDir(), BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);
          try {
            if (!remoteData.exists()) {
              Log.d(TAG, "From assets: " + BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);
              try (InputStream inputStream = context.getAssets().open(BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE)) {
                try (FileOutputStream outputStream = new FileOutputStream(sData)) {
                  byte[] buf = new byte[1024];
                  int len;
                  while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                  }
                }
              }
            } else {
              Log.d(TAG, "From remote: " + BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);
              try (InputStream inputStream = new FileInputStream(remoteData)) {
                try (FileOutputStream outputStream = new FileOutputStream(sData)) {
                  byte[] buf = new byte[1024];
                  int len;
                  while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                  }
                }
              }
            }
          } catch (IOException ioe) {
            Log.w(TAG, "Could not get assets.", ioe);
          } finally {
            if (remoteData.exists() && !remoteData.delete()) {
              Log.w(TAG, "Could not remove local copy of remote " + BaseActivity.DEFAULT_PUBLISHER_SERIES_FILE);
            }
          }

          INSTANCE = Room.databaseBuilder(context.getApplicationContext(), CollectorDatabase.class, BaseActivity.DATABASE_NAME)
            .addCallback(sRoomDatabaseCallback)
            .build();
        }
      }
    }

    return INSTANCE;
  }

  private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {

    @Override
    public void onOpen(@NonNull SupportSQLiteDatabase db) {
      super.onOpen(db);

      Log.d(TAG, "++onOpen(SupportSQLiteDatabase)");
      new PopulateDbAsync(INSTANCE, sData, sLibrary).execute();
    }
  };

  private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

    private final ComicBookDao mComicBookDao;
    private final File mData;
    private final File mLibrary;
    private final PublisherDao mPublisherDao;
    private final SeriesDao mSeriesDao;

    PopulateDbAsync(CollectorDatabase db, File data, File library) {

      mComicBookDao = db.comicBookDao();
      mData = data;
      mLibrary = library;
      mPublisherDao = db.publisherDao();
      mSeriesDao = db.seriesDao();
    }

    @Override
    protected Void doInBackground(final Void... params) {

      if (mData.exists() && mData.canRead()) {
        Log.d(TAG, "Loading " + mData.getAbsolutePath());
        RemoteData remoteData = null;
        try (Reader reader = new FileReader(mData.getAbsolutePath())) {
          Gson gson = new Gson();
          Type collectionType = new TypeToken<RemoteData>() {
          }.getType();
          remoteData = gson.fromJson(reader, collectionType);
        } catch (FileNotFoundException e) {
          Log.w(TAG, "Source data from server not found locally.");
        } catch (IOException e) {
          Log.w(TAG, "Could not read the source data.");
        }

        if (remoteData != null) {
          String message = "Publisher data processed:";
          int count = 0;
          try {
            for (PublisherEntity publisher : remoteData.ComicPublishers) {
              mPublisherDao.insert(publisher);
              message = String.format(Locale.US, "%s %d...", message, ++count);
            }
          } catch (Exception e) {
            Log.w(TAG, "Could not process publisher data.", e);
          } finally {
            Log.d(TAG, message);
          }

          message = "Series data processed:";
          count = 0;
          try {
            for (SeriesEntity series : remoteData.ComicSeries) {
              mSeriesDao.insert(series);
              message = String.format(Locale.US, "%s %d...", message, ++count);
            }
          } catch (Exception e) {
            Log.w(TAG, "Could not process series data.", e);
          } finally {
            Log.d(TAG, message);
          }
        } else {
          Log.e(TAG, "Source data was incomplete.");
        }
      } else {
        Log.e(TAG, "%s does not exist yet: " + sData.getAbsoluteFile());
      }

      if (mLibrary != null && mLibrary.exists()) {
        Log.d(TAG, "Loading " + mLibrary.getAbsoluteFile());
        try (Reader reader = new FileReader(mLibrary.getAbsolutePath())) {
          Gson gson = new Gson();
          Type collectionType = new TypeToken<ArrayList<ComicBookEntity>>() {}.getType();
          ArrayList<ComicBookEntity> comicBookList = gson.fromJson(reader, collectionType);
          Log.d(TAG, "Migrating ComicBook(s) to database: " + comicBookList.size());
          for (ComicBookEntity comic : comicBookList) {
            if (comic.IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length()) {
              try {
//                comic.ProductCode = String.format(Locale.US, "%s%s", comic.PublisherId, comic.SeriesId);
                comic.Id = String.format(Locale.US, "%s-%s", comic.ProductCode, comic.IssueCode);
              } catch (Exception e) {
                comic.Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
                comic.ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
                comic.IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
//                comic.PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
//                comic.SeriesId = BaseActivity.DEFAULT_SERIES_ID;
              }
            } else {
              comic.Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
              comic.ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
              comic.IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
//              comic.PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
//              comic.SeriesId = BaseActivity.DEFAULT_SERIES_ID;
            }

            mComicBookDao.insert(comic);
          }
        } catch (Exception e) {
          Log.e(TAG, "Failed reading support data.", e);
        }

        if (mLibrary.delete()) {
          Log.d(TAG, "Removing old library file.");
        } else {
          Log.w(TAG, "Could not remove old library file.");
        }
      }

      return null;
    }
  }
}
