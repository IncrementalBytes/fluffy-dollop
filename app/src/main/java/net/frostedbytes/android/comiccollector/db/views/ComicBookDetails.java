package net.frostedbytes.android.comiccollector.db.views;

import androidx.room.DatabaseView;
import java.io.Serializable;
import java.util.Locale;
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
  public String SeriesTitle;
  public String PublisherName;
  public boolean IsOwned;
  public boolean IsRead;
  public int Volume;

  public ComicBookDetails() {

    Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
    IsOwned = false;
    IsRead = false;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    IssueNumber = 0;
    ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
    Published = BaseActivity.DEFAULT_PUBLISHED_DATE;
    PublisherName = "";
    SeriesTitle = "";
    Title = "";
    Volume = 0;
  }

  public ComicBookDetails(ComicBook comicBook) {

    Id = comicBook.Id;
    IsOwned = comicBook.IsOwned;
    IsRead = comicBook.IsRead;
    IssueCode = comicBook.IssueCode;
    IssueNumber = comicBook.IssueNumber;
    Published = comicBook.PublishedDate;
    PublisherName = "";
    ProductCode = comicBook.ProductCode;
    SeriesTitle = "";
    Title = comicBook.Title;
    Volume = 0;
  }

  public ComicBookDetails(ComicBookDetails comicBookDetails) {

    Id = comicBookDetails.Id;
    IsOwned = comicBookDetails.IsOwned;
    IsRead = comicBookDetails.IsRead;
    IssueCode = comicBookDetails.IssueCode;
    IssueNumber = comicBookDetails.IssueNumber;
    ProductCode = comicBookDetails.ProductCode;
    Published = comicBookDetails.Published;
    PublisherName = comicBookDetails.PublisherName;
    SeriesTitle = comicBookDetails.SeriesTitle;
    Title = comicBookDetails.Title;
    Volume = comicBookDetails.Volume;
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "ComicBook { Title=%s, ProductCode=%s, Issue=%d, %s, %s}",
      Title,
      ProductCode,
      IssueNumber,
      IsOwned ? "Owned" : "OnWishlist",
      IsRead ? "Read" : "Unread");
  }
}
