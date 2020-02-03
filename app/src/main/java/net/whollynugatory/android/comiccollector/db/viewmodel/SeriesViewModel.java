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

package net.whollynugatory.android.comiccollector.db.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.whollynugatory.android.comiccollector.db.CollectorDatabase;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.repository.SeriesRepository;

public class SeriesViewModel extends AndroidViewModel {

  private SeriesRepository mRepository;
  private LiveData<List<SeriesEntity>> mAllSeries;

  public SeriesViewModel(Application application) {
    super(application);

    mRepository = SeriesRepository.getInstance(CollectorDatabase.getInstance(application).seriesDao());
    mAllSeries = mRepository.getAll();
  }

  public void delete(String volumeId) {

    mRepository.delete(volumeId);
  }

  public LiveData<List<SeriesEntity>> getAll() {

    return mAllSeries;
  }

  public void insert(SeriesEntity publisherEntity) {

    mRepository.insert(publisherEntity);
  }

  public void insertAll(List<SeriesEntity> publisherEntityList) {

    mRepository.insertAll(publisherEntityList);
  }

  public void update(SeriesEntity publisherEntity) {

    mRepository.update(publisherEntity);
  }
}
