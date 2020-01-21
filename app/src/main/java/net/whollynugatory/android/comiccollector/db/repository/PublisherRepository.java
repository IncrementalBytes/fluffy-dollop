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
import net.whollynugatory.android.comiccollector.db.dao.PublisherDao;
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;

public class PublisherRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "PublisherRepository";

  private static volatile PublisherRepository INSTANCE;

  private PublisherDao mPublisherDao;

  private PublisherRepository(PublisherDao publisherDao) {

    mPublisherDao = publisherDao;
  }

  public static PublisherRepository getInstance(final PublisherDao publisherDao) {

    if (INSTANCE == null) {
      synchronized (PublisherRepository.class) {
        if (INSTANCE == null) {
          Log.d(TAG, "++getInstance(Context)");
          INSTANCE = new PublisherRepository(publisherDao);
        }
      }
    }

    return INSTANCE;
  }

  public void delete(String publisherId) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mPublisherDao.delete(publisherId));
  }

  public LiveData<PublisherEntity> get(String publisherId) {

    return mPublisherDao.get(publisherId);
  }

  public LiveData<List<PublisherEntity>> getAll() {

    return mPublisherDao.getAll();
  }

  public void insert(PublisherEntity publisherEntity) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mPublisherDao.insert(publisherEntity));
  }

  public void insertAll(List<PublisherEntity> publisherEntityList) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mPublisherDao.insertAll(publisherEntityList));
  }

  public void update(PublisherEntity publisherEntity) {

    CollectorDatabase.databaseWriteExecutor.execute(() -> mPublisherDao.update(publisherEntity));
  }
}
