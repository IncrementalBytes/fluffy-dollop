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
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.repository.PublisherRepository;

public class PublisherViewModel extends AndroidViewModel {

  private PublisherRepository mRepository;
  private LiveData<List<PublisherEntity>> mAllPublishers;

  public PublisherViewModel(Application application) {
    super(application);

    mRepository = PublisherRepository.getInstance(CollectorDatabase.getInstance(application).publisherDao());
    mAllPublishers = mRepository.getAll();
  }

  public void delete(String volumeId) {

    mRepository.delete(volumeId);
  }

  public LiveData<List<PublisherEntity>> getAlll() {

    return mAllPublishers;
  }

  public void insert(PublisherEntity publisherEntity) {

    mRepository.insert(publisherEntity);
  }

  public void insertAll(List<PublisherEntity> publisherEntityList) {

    mRepository.insertAll(publisherEntityList);
  }

  public void update(PublisherEntity publisherEntity) {

    mRepository.update(publisherEntity);
  }
}
