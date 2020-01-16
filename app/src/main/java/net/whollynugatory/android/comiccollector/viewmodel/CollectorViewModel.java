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
package net.whollynugatory.android.comiccollector.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.whollynugatory.android.comiccollector.db.CollectorRepository;
import net.whollynugatory.android.comiccollector.db.entity.ComicBook;
import net.whollynugatory.android.comiccollector.db.entity.ComicPublisher;
import net.whollynugatory.android.comiccollector.db.entity.ComicSeries;
import net.whollynugatory.android.comiccollector.db.views.ComicBookDetails;
import net.whollynugatory.android.comiccollector.db.views.ComicSeriesDetails;

public class CollectorViewModel extends AndroidViewModel {

  private CollectorRepository mRepository;
  private LiveData<List<ComicBookDetails>> mAllComicBooks;

  public CollectorViewModel(Application application) {
    super(application);

    mRepository = new CollectorRepository(application);

    mAllComicBooks = mRepository.getComicBooks();
  }

  public void deleteAllComicBooks() {

    mRepository.deleteAllComicBooks();
  }

  public void deleteComicBookById(String comicBookId) {

    mRepository.deleteComicBookById(comicBookId);
  }

  public LiveData<List<ComicBook>> exportable() {

    return mRepository.exportable();
  }

  public LiveData<ComicBookDetails> getComicBookById(String productCode, String issueCode) {

    return mRepository.getComicBookById(productCode, issueCode);
  }

  public LiveData<List<ComicBookDetails>> getComicBooks() {

    return mAllComicBooks;
  }

  public LiveData<List<ComicBookDetails>> getComicBooksByProductCode(String productCode) {

    return mRepository.getComicBooksByProductCode(productCode);
  }

  public LiveData<ComicPublisher> getComicPublisherById(String publisherId) {

    return mRepository.getComicPublisherById(publisherId);
  }

  public LiveData<ComicSeriesDetails> getComicSeriesByProductCode(String productCode) {

    return mRepository.getComicSeriesByProductCode(productCode);
  }

  public void insert(ComicBook comicBook) {

    mRepository.insert(comicBook);
  }

  public void insert(ComicSeries comicSeries) {

    mRepository.insert(comicSeries);
  }

  public void insertAll(List<ComicBook> comicBooks) {

    mRepository.insertAll(comicBooks);
  }
}
