package net.frostedbytes.android.comiccollector.db.views;

import androidx.room.DatabaseView;
import java.io.Serializable;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.db.entity.ComicBook;

@DatabaseView(
  "SELECT Book.id AS Id, " +
    "Book.product_code AS ProductCode, " +
    "Book.issue_code AS IssueCode," +
    "Book.title AS Title, " +
    "Book.issue_number AS IssueNumber, " +
    "Book.published_date AS Published, " +
    "Book.owned AS IsOwned, " +
    "Book.read AS IsRead, " +
    "Series.id AS SeriesId, " +
    "Series.title AS SeriesTitle, " +
    "Series.volume AS Volume, " +
    "Publishers.name AS PublisherName " +
  "FROM comic_book_table AS Book " +
  "INNER JOIN comic_series_table AS Series ON Series.id = Book.product_code " +
  "INNER JOIN comic_publisher_table AS Publishers ON Series.publisher_id = Publishers.id")
public class ComicBookDetails implements Serializable {

  public String Id;
  public String ProductCode;
  public String IssueCode;
  public String Title;
  public int IssueNumber;
  public String Published;
  public String SeriesId;
  public String SeriesTitle;
  public String PublisherName;
  public boolean IsOwned;
  public boolean IsRead;
  public int Volume;

  public ComicBookDetails() {

    Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
    ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    Title = "";
    IssueNumber = 0;
    Published = BaseActivity.DEFAULT_PUBLISHED_DATE;
    SeriesId = BaseActivity.DEFAULT_COMIC_SERIES_ID;
    SeriesTitle = "";
    PublisherName = "";
    IsOwned = false;
    IsRead = false;
    Volume = 0;
  }

  public ComicBookDetails(ComicBook comicBook) {

    Id = comicBook.Id;
    ProductCode = comicBook.ProductCode;
    IssueCode = comicBook.IssueCode;
    Title = comicBook.Title;
    IssueNumber = comicBook.IssueNumber;
    Published = comicBook.PublishedDate;
    SeriesId = comicBook.SeriesId;
    //PublisherName = comicBook.PublisherId;
    IsOwned = comicBook.IsOwned;
    IsRead = comicBook.IsRead;
    //Volume = 0;
  }

  public ComicBookDetails(ComicBookDetails comicBookDetails) {

    Id = comicBookDetails.Id;
    ProductCode = comicBookDetails.ProductCode;
    IssueCode = comicBookDetails.IssueCode;
    Title = comicBookDetails.Title;
    IssueNumber = comicBookDetails.IssueNumber;
    Published = comicBookDetails.Published;
    SeriesId = comicBookDetails.SeriesId;
    SeriesTitle = comicBookDetails.SeriesTitle;
    PublisherName = comicBookDetails.PublisherName;
    IsOwned = comicBookDetails.IsOwned;
    IsRead = comicBookDetails.IsRead;
    Volume = comicBookDetails.Volume;
  }
}
