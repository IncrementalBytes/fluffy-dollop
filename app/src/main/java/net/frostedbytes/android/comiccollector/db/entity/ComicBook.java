package net.frostedbytes.android.comiccollector.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;

@Entity(
  tableName = "comic_book_table",
  foreignKeys = {@ForeignKey(entity = ComicSeries.class, parentColumns = "id", childColumns = "product_code")},
    indices = {@Index(value = {"product_code", "issue_code"}, unique = true)})
public class ComicBook {

  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public String Id;

  @ColumnInfo(name = "product_code")
  @SerializedName("product_code")
  public String ProductCode;

  @NonNull
  @ColumnInfo(name = "issue_code")
  @SerializedName("issue_code")
  public String IssueCode;

  @ColumnInfo(name = "issue_number")
  public int IssueNumber;

  @ColumnInfo(name = "cover_variant")
  public int CoverVariant;

  @ColumnInfo(name = "print_run")
  public int PrintRun;

  @ColumnInfo(name = "owned")
  @SerializedName("owned")
  public boolean IsOwned;

  @ColumnInfo(name = "read")
  @SerializedName("read")
  public boolean IsRead;

  @NonNull
  @ColumnInfo(name = "title")
  @SerializedName("title")
  public String Title;

  @NonNull
  @ColumnInfo(name = "published_date")
  @SerializedName("publish_date")
  public String PublishedDate;

  @Ignore
  @SerializedName("publish_id")
  public String PublisherId;

  @Ignore
  @SerializedName("series_id")
  public String SeriesId;

  public ComicBook() {

    Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
    ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
    CoverVariant = -1;
    IsOwned = false;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    IssueNumber = -1;
    PrintRun = -1;
    PublishedDate = "";
    PublisherId = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
    IsRead = false;
    SeriesId = BaseActivity.DEFAULT_COMIC_SERIES_ID;
    Title = "";
  }

  public ComicBook(
    @NonNull String id,
    @NonNull String productCode,
    String publisherId,
    @NonNull String seriesId,
    @NonNull String issueCode,
    boolean owned,
    boolean read,
    @NonNull String title,
    @NonNull String publishedDate) {

    Id = id;
    ProductCode = productCode;
    IssueCode = issueCode;
    IsOwned = owned;
    IsRead = read;
    Title = title;
    PublishedDate = publishedDate;

    PublisherId = publisherId;
    SeriesId = seriesId;
  }

  public ComicBook(ComicBook comicBook) {

    Id = comicBook.Id;
    ProductCode = comicBook.ProductCode;
    IssueCode = comicBook.IssueCode;
    IsOwned = comicBook.IsOwned;
    IsRead = comicBook.IsRead;
    Title = comicBook.Title;
    PublishedDate = comicBook.PublishedDate;

    CoverVariant = comicBook.CoverVariant;
    IssueNumber = comicBook.IssueNumber;
    PrintRun = comicBook.PrintRun;
    PublisherId = comicBook.PublisherId;
    SeriesId = comicBook.SeriesId;
  }

  /*
    Public Method(s)
   */
  /**
   * Attempts to extract the Publisher, Series, and IssueCode identifiers from a product code.
   * @param productCode 12-5 character string representing the full product code.
   */
  public void parseProductCode(String productCode) {

    String[] segments = productCode.split("-");
    ProductCode = segments[0];
    if (ProductCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length() && !ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE)) {
      try {
        Id = String.format(Locale.US, "%s-%s", ProductCode, BaseActivity.DEFAULT_ISSUE_CODE);
        PublisherId = ProductCode.substring(0, BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
        SeriesId = ProductCode.substring(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
      } catch (Exception e) {
        Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
        ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
        PublisherId = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
        SeriesId = BaseActivity.DEFAULT_COMIC_SERIES_ID;
      }
    }

    if (segments.length > 1) { // grab issue number
      Id = productCode;
      IssueCode = segments[1];
      if (IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length() && !IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE)) {
        try {
          String temp = IssueCode.substring(0, IssueCode.length() - 2);
          IssueNumber = Integer.parseInt(temp);
          temp = IssueCode.substring(IssueCode.length() - 2, IssueCode.length() - 1);
          CoverVariant = Integer.parseInt(temp);
          temp = IssueCode.substring(IssueCode.length() -1);
          PrintRun = Integer.parseInt(temp);
        } catch (Exception e) {
          Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
          ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
          IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
          IssueNumber = -1;
          CoverVariant = -1;
          PrintRun = -1;
        }
      } else {
        Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
        ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
        IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
        IssueNumber = -1;
        CoverVariant = -1;
        PrintRun = -1;
      }
    }
  }

  /**
   * Validates the properties of the comic book.
   * @return TRUE if all properties are within expected parameters, otherwise FALSE.
   */
  public boolean isValid() {

    return !Id.equals(BaseActivity.DEFAULT_COMIC_BOOK_ID) &&
      Id.length() == BaseActivity.DEFAULT_COMIC_BOOK_ID.length() &&
      CoverVariant > 0 && IssueNumber > 0 && PrintRun > 0;
  }
}
