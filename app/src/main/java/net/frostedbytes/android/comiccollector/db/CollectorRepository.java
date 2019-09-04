package net.frostedbytes.android.comiccollector.db;

import android.app.Application;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.frostedbytes.android.comiccollector.db.dao.ComicBookDao;
import net.frostedbytes.android.comiccollector.db.dao.ComicPublisherDao;
import net.frostedbytes.android.comiccollector.db.dao.ComicSeriesDao;
import net.frostedbytes.android.comiccollector.db.entity.ComicBook;
import net.frostedbytes.android.comiccollector.db.entity.ComicPublisher;
import net.frostedbytes.android.comiccollector.db.entity.ComicSeries;
import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;

public class CollectorRepository {

  private LiveData<List<ComicBookDetails>> mComicBooksExtended;

  private ComicBookDao mComicBookDao;
  private ComicPublisherDao mComicPublisherDao;
  private ComicSeriesDao mComicSeriesDao;

  public CollectorRepository(Application application) {

    CollectorRoomDatabase db = CollectorRoomDatabase.getDatabase(application);
    mComicBookDao = db.bookDao();
    mComicPublisherDao = db.publisherDao();
    mComicSeriesDao = db.seriesDao();

    mComicBooksExtended = mComicBookDao.getAll();
  }

  public void deleteAllComicBooks() {

    new deleteAllComicBooksAsyncTask(mComicBookDao).execute();
  }

  public void deleteComicBookById(String comicBookId) {

    new deleteComicBookByIdAsyncTask(mComicBookDao).execute(comicBookId);
  }

  public LiveData<List<ComicBook>> exportable() {

    return mComicBookDao.exportable();
  }

  public LiveData<ComicBookDetails> getComicBookById(String comicBookId, String issueCode) {

    return mComicBookDao.get(comicBookId, issueCode);
  }

  public LiveData<List<ComicBookDetails>> getComicBooks() {

    return mComicBooksExtended;
  }

  public LiveData<List<ComicBookDetails>> getComicBooksByProductCode(String productCode) {

    return mComicBookDao.getByProductCode(productCode);
  }

  public LiveData<ComicPublisher> getComicPublisherById(String publisherId) {

    return mComicPublisherDao.get(publisherId);
  }

  public LiveData<ComicSeriesDetails> getComicSeriesByProductCode(String productCode) {

    return mComicSeriesDao.get(productCode);
  }

  public void insert(ComicBook comicBook) {

    new insertComicBookAsyncTask(mComicBookDao).execute(comicBook);
  }

  public void insert(ComicPublisher publisher) {

    new insertComicPublisherAsyncTask(mComicPublisherDao).execute(publisher);
  }

  public void insert(ComicSeries series) {

    new insertComicSeriesAsyncTask(mComicSeriesDao).execute(series);
  }

  @SuppressWarnings({"unchecked", "varargs"})
  public void insertAll(List<ComicBook> comicBooks) {

    new insertAllComicBookAsyncTask(mComicBookDao).execute(comicBooks);
  }

  private static class deleteAllComicBooksAsyncTask extends AsyncTask<Void, Void, Void> {

    private ComicBookDao mAsyncTaskDao;

    deleteAllComicBooksAsyncTask(ComicBookDao dao) {

      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final Void... params) {

      mAsyncTaskDao.deleteAll();
      return null;
    }
  }

  private static class deleteComicBookByIdAsyncTask extends AsyncTask<String, Void, Void> {

    private ComicBookDao mAsyncTaskDao;

    deleteComicBookByIdAsyncTask(ComicBookDao dao) {

      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final String... params) {

      mAsyncTaskDao.deleteById(params[0]);
      return null;
    }
  }

  private static class insertComicBookAsyncTask extends AsyncTask<ComicBook, Void, Void> {

    private ComicBookDao mAsyncTaskDao;

    insertComicBookAsyncTask(ComicBookDao dao) {

      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final ComicBook... params) {

      mAsyncTaskDao.insert(params[0]);
      return null;
    }
  }

  private static class insertComicPublisherAsyncTask extends AsyncTask<ComicPublisher, Void, Void> {

    private ComicPublisherDao mAsyncTaskDao;

    insertComicPublisherAsyncTask(ComicPublisherDao dao) {

      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final ComicPublisher... params) {

      mAsyncTaskDao.insert(params[0]);
      return null;
    }
  }

  private static class insertComicSeriesAsyncTask extends AsyncTask<ComicSeries, Void, Void> {

    private ComicSeriesDao mAsyncTaskDao;

    insertComicSeriesAsyncTask(ComicSeriesDao dao) {

      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final ComicSeries... params) {

      mAsyncTaskDao.insert(params[0].Id, params[0].PublisherId, params[0].SeriesId, params[0].Title, params[0].Volume);
      return null;
    }
  }

  private static class insertAllComicBookAsyncTask extends AsyncTask<List<ComicBook>, Void, Void> {

    private ComicBookDao mAsyncTaskDao;

    insertAllComicBookAsyncTask(ComicBookDao dao) {

      mAsyncTaskDao = dao;
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(final List<ComicBook>... params) {

      mAsyncTaskDao.insertAll(params[0]);
      return null;
    }
  }
}
