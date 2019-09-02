package net.frostedbytes.android.comiccollector.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.frostedbytes.android.comiccollector.db.CollectorRepository;
import net.frostedbytes.android.comiccollector.db.entity.ComicBook;
import net.frostedbytes.android.comiccollector.db.entity.ComicPublisher;
import net.frostedbytes.android.comiccollector.db.entity.ComicSeries;
import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;

public class CollectorViewModel extends AndroidViewModel {

  private CollectorRepository mRepository;
  private LiveData<List<ComicBookDetails>> mAllComicBooks;

  public CollectorViewModel(Application application) {
    super(application);

    mRepository = new CollectorRepository(application);

    mAllComicBooks = mRepository.getComicBooks();
  }

  public void deleteComicBookById(String comicBookId) {

    mRepository.deleteComicBookById(comicBookId);
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
}
