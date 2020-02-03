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

package net.whollynugatory.android.comiccollector.db.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.db.CollectorDatabase;
import net.whollynugatory.android.comiccollector.db.dao.ComicBookDao;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;

public class ComicBookRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBookRepository";

  private static volatile ComicBookRepository INSTANCE;

  private ComicBookDao mComicBookDao;

  private ComicBookRepository(ComicBookDao comicBookDao) {

    mComicBookDao = comicBookDao;
  }

  public static ComicBookRepository getInstance(final ComicBookDao comicBookDao) {

    if (INSTANCE == null) {
      synchronized (ComicBookRepository.class) {
        if (INSTANCE == null) {
          Log.d(TAG, "++getInstance(Context)");
          INSTANCE = new ComicBookRepository(comicBookDao);
        }
      }
    }

    return INSTANCE;
  }

  public void delete(String volumeId) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mComicBookDao.deleteById(volumeId));
  }

  public LiveData<List<ComicBookEntity>> exportable() {

    return mComicBookDao.exportable();
  }

  public LiveData<List<ComicBookEntity>> find(String productCode) {

    return mComicBookDao.find(productCode);
  }

  public LiveData<ComicBookEntity> find(String productCode, String issueCode) {

    return mComicBookDao.find(productCode, issueCode);
  }

  public LiveData<List<ComicBookEntity>> getRecent() {

    return mComicBookDao.getAllByRecent();
  }

  public void insert(ComicBookEntity comicBookEntity) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mComicBookDao.insert(comicBookEntity));
  }

  public void insertAll(List<ComicBookEntity> comicBookEntityList) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mComicBookDao.insertAll(comicBookEntityList));
  }

  public void update(ComicBookEntity comicBookEntity) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mComicBookDao.update(comicBookEntity));
  }
}
