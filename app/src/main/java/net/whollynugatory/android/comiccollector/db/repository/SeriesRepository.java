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
import net.whollynugatory.android.comiccollector.db.dao.SeriesDao;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;

public class SeriesRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "SeriesRepository";

  private static volatile SeriesRepository INSTANCE;

  private SeriesDao mSeriesDao;

  private SeriesRepository(SeriesDao seriesDao) {

    mSeriesDao = seriesDao;
  }

  public static SeriesRepository getInstance(final SeriesDao seriesDao) {

    if (INSTANCE == null) {
      synchronized (SeriesRepository.class) {
        if (INSTANCE == null) {
          Log.d(TAG, "++getInstance(Context)");
          INSTANCE = new SeriesRepository(seriesDao);
        }
      }
    }

    return INSTANCE;
  }

  public void delete(String id) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mSeriesDao.delete(id));
  }

  public LiveData<SeriesEntity> get(String publisherCode, String seriesCode) {

    return mSeriesDao.get(publisherCode, seriesCode);
  }

  public LiveData<List<SeriesEntity>> getAll() {

    return mSeriesDao.getAll();
  }

  public void insert(SeriesEntity seriesEntity) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mSeriesDao.insert(seriesEntity));
  }

  public void insertAll(List<SeriesEntity> seriesEntityList) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mSeriesDao.insertAll(seriesEntityList));
  }

  public void update(SeriesEntity seriesEntity) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mSeriesDao.update(seriesEntity));
  }
}
