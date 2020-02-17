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
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.repository.ComicBookRepository;
import net.whollynugatory.android.comiccollector.db.repository.ComicDetailsRepository;
import net.whollynugatory.android.comiccollector.db.repository.SeriesRepository;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;

public class CollectorViewModel extends AndroidViewModel {

  private ComicBookRepository mComicBookRepository;
  private ComicDetailsRepository mComicDetailsRepository;
  private SeriesRepository mSeriesRepository;

  private LiveData<List<ComicDetails>> mComicDetails;
  private LiveData<List<SeriesEntity>> mSeriesEntities;

  public CollectorViewModel(Application application) {
    super(application);

    mComicBookRepository = ComicBookRepository.getInstance(CollectorDatabase.getInstance(application).comicBookDao());
    mComicDetailsRepository = ComicDetailsRepository.getInstance(CollectorDatabase.getInstance(application).comicDetailsDao());
    mSeriesRepository = SeriesRepository.getInstance(CollectorDatabase.getInstance(application).seriesDao());

    mComicDetails = mComicDetailsRepository.getRecent();
    mSeriesEntities = mSeriesRepository.getAll();
  }

  public void deleteComicById(String fullId) {

    mComicDetailsRepository.deleteById(fullId);
  }

  public LiveData<ComicDetails> findComic(String productCode, String issueCode) {

    return mComicDetailsRepository.find(productCode, issueCode);
  }

  public LiveData<List<ComicDetails>> findComicsByProductCode(String productCode) {

    return mComicDetailsRepository.findByProductCode(productCode);
  }

  public LiveData<SeriesEntity> findSeries(String seriesId) {

    return mSeriesRepository.get(seriesId);
  }

  public LiveData<List<ComicDetails>> getRecentComics() {

    return mComicDetails;
  }

  public LiveData<List<SeriesEntity>> getRecentSeries() {

    return mSeriesEntities;
  }

  public void insertComicBook(ComicBookEntity comicBookEntity) {

    mComicBookRepository.insert(comicBookEntity);
  }

  public void insertSeries(SeriesEntity seriesEntity) {

    mSeriesRepository.insert(seriesEntity);
  }
}
