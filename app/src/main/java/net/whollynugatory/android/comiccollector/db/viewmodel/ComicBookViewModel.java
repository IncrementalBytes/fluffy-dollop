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
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;
import net.whollynugatory.android.comiccollector.db.repository.ComicBookRepository;

public class ComicBookViewModel extends AndroidViewModel {

  private ComicBookRepository mRepository;
  private LiveData<List<ComicBookEntity>> mAllBooks;

  public ComicBookViewModel(Application application) {
    super(application);

    mRepository = ComicBookRepository.getInstance(CollectorDatabase.getInstance(application).comicBookDao());
    mAllBooks = mRepository.getRecent();
  }

  public void delete(String volumeId) {

    mRepository.delete(volumeId);
  }

  public LiveData<List<ComicBookEntity>> exportable() {

    return mRepository.exportable();
  }

  public LiveData<List<ComicBookEntity>> find(String productCode) {

    return mRepository.find(productCode);
  }

  public LiveData<ComicBookEntity> find(String productCode, String issueCode) {

    return mRepository.find(productCode, issueCode);
  }

  public LiveData<List<ComicBookEntity>> getRecent() {

    return mAllBooks;
  }

  public void insert(ComicBookEntity comicBookEntity) {

    mRepository.insert(comicBookEntity);
  }

  public void insertAll(List<ComicBookEntity> comicBookEntityList) {

    mRepository.insertAll(comicBookEntityList);
  }

  public void update(ComicBookEntity comicBookEntity) {

    mRepository.update(comicBookEntity);
  }
}
