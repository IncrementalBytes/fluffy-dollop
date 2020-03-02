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
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.repository.ComicBookRepository;
import net.whollynugatory.android.comiccollector.db.repository.ComicDetailsRepository;
import net.whollynugatory.android.comiccollector.db.repository.PublisherRepository;
import net.whollynugatory.android.comiccollector.db.repository.SeriesDetailsRepository;
import net.whollynugatory.android.comiccollector.db.repository.SeriesRepository;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;

public class CollectorViewModel extends AndroidViewModel {

  private ComicBookRepository mComicBookRepository;
  private ComicDetailsRepository mComicDetailsRepository;
  private PublisherRepository mPublisherRepository;
  private SeriesRepository mSeriesRepository;
  private SeriesDetailsRepository mSeriesDetailsRepository;

  private LiveData<List<ComicDetails>> mComicDetails;
  private LiveData<List<SeriesDetails>> mSeriesDetails;

  public CollectorViewModel(Application application) {
    super(application);

    mComicBookRepository = ComicBookRepository.getInstance(CollectorDatabase.getInstance(application).comicBookDao());
    mComicDetailsRepository = ComicDetailsRepository.getInstance(CollectorDatabase.getInstance(application).comicDetailsDao());
    mPublisherRepository = PublisherRepository.getInstance(CollectorDatabase.getInstance(application).publisherDao());
    mSeriesRepository = SeriesRepository.getInstance(CollectorDatabase.getInstance(application).seriesDao());
    mSeriesDetailsRepository = SeriesDetailsRepository.getInstance(CollectorDatabase.getInstance(application).seriesDetailsDao());

    mComicDetails = mComicDetailsRepository.getRecent();
    mSeriesDetails = mSeriesDetailsRepository.getAll();
  }

  public void deleteAllComicBooks() {

    mComicBookRepository.deleteAll();
  }

  public void deleteComicById(String id) {

    mComicBookRepository.delete(id);
  }

  public LiveData<List<ComicBookEntity>> exportComics() {

    return mComicBookRepository.exportable();
  }

  public LiveData<ComicDetails> getComic(String publisherCode, String seriesCode, String issueCode) {

    return mComicDetailsRepository.get(publisherCode, seriesCode, issueCode);
  }

  public LiveData<List<ComicDetails>> getComicsBySeriesId(String seriesId) {

    return mComicDetailsRepository.getBySeriesId(seriesId);
  }

  public LiveData<PublisherEntity> getPublisherById(String publisherId) {

    return mPublisherRepository.getById(publisherId);
  }

  public LiveData<List<ComicDetails>> getRecentComics() {

    return mComicDetails;
  }

  public LiveData<SeriesDetails> getSeries(String publisherCode, String seriesCode) {

    return mSeriesDetailsRepository.get(publisherCode, seriesCode);
  }

  public LiveData<List<SeriesDetails>> getSeries() {

    return mSeriesDetails;
  }

  public LiveData<List<SeriesDetails>> getSeriesByPublisher(String publisherCode) {

    return mSeriesDetailsRepository.getAllByPublisher(publisherCode);
  }

  public void insertComicBook(ComicBookEntity comicBookEntity) {

    mComicBookRepository.insert(comicBookEntity);
  }

  public void insertPublisher(PublisherEntity publisherEntity) {

    mPublisherRepository.insert(publisherEntity);
  }

  public void insertSeries(SeriesEntity seriesEntity) {

    mSeriesRepository.insert(seriesEntity);
  }

  public void updateComic(ComicBookEntity comicBookEntity) {

    mComicBookRepository.update(comicBookEntity);
  }
}
