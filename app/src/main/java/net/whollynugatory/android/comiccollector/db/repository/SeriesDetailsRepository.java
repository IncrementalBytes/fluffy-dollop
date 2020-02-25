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
import net.whollynugatory.android.comiccollector.db.dao.SeriesDetailsDao;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class SeriesDetailsRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "SeriesDetailsRepository";

  private static volatile SeriesDetailsRepository INSTANCE;

  private SeriesDetailsDao mSeriesDetailsDao;

  private SeriesDetailsRepository(SeriesDetailsDao seriesDetailsDao) {

    mSeriesDetailsDao = seriesDetailsDao;
  }

  public static SeriesDetailsRepository getInstance(final SeriesDetailsDao seriesDetailsDao) {

    if (INSTANCE == null) {
      synchronized (SeriesDetailsRepository.class) {
        if (INSTANCE == null) {
          Log.d(TAG, "++getInstance(Context)");
          INSTANCE = new SeriesDetailsRepository(seriesDetailsDao);
        }
      }
    }

    return INSTANCE;
  }

  public LiveData<SeriesDetails> get(String publisherCode, String seriesCode) {

    return mSeriesDetailsDao.get(publisherCode, seriesCode);
  }

  public LiveData<List<SeriesDetails>> getAll() {

    return mSeriesDetailsDao.getAll();
  }

  public LiveData<List<SeriesDetails>> getAllByPublisher(String publisherCode) {

    return mSeriesDetailsDao.getAllByPublisher(publisherCode);
  }
}
