package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import net.frostedbytes.android.comiccollector.BaseActivity;

import java.util.Locale;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class ComicBook implements Parcelable {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBook";

  /**
   * Cover version of comic.
   */
  public int CoverVersion;

  /**
   * Unique code for issue (paired with SeriesId); populates IssueNumber, CoverVersion, and PrintRun.
   */
  @SerializedName("issue_code")
  public String IssueCode;

  /**
   * Specific issue number.
   */
  public int IssueNumber;

  /**
   * Whether or not comic is owned by the user, otherwise it's on their wishlist.
   */
  @SerializedName("owned")
  public boolean OwnedState;

  /**
   * Print run number.
   */
  public int PrintRun;

  /**
   * Date comic was published.
   */
  @SerializedName("publish_date")
  public String PublishedDate;

  /**
   * Unique identifier for publisher of comic.
   */
  @SerializedName("publish_id")
  public String PublisherId;

  /**
   * Whether or not comic is read by the user, otherwise it's unread.
   */
  @SerializedName("read")
  public boolean ReadState;

  /**
   * Unique identifier for the series this comic is published under.
   */
  @SerializedName("series_id")
  public String SeriesId;

  /**
   * Title for comic; can be blank.
   */
  @SerializedName("title")
  public String Title;

  public ComicBook() {

    CoverVersion = -1;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    IssueNumber = -1;
    OwnedState = false;
    PrintRun = -1;
    PublishedDate = "00/0000";
    PublisherId = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
    ReadState = false;
    SeriesId = BaseActivity.DEFAULT_COMIC_SERIES_ID;
    Title = "";
  }

  public ComicBook(ComicBook comicBook) {

    CoverVersion = comicBook.CoverVersion;
    IssueCode = comicBook.IssueCode;
    IssueNumber = comicBook.IssueNumber;
    OwnedState = comicBook.OwnedState;
    PrintRun = comicBook.PrintRun;
    PublisherId = comicBook.PublisherId;
    PublishedDate = comicBook.PublishedDate;
    ReadState = comicBook.ReadState;
    SeriesId = comicBook.SeriesId;
    Title = comicBook.Title;
  }

  protected ComicBook(Parcel in) {

    CoverVersion = in.readInt();
    IssueCode = in.readString();
    IssueNumber = in.readInt();
    OwnedState = in.readInt() != 0;
    PrintRun = in.readInt();
    PublisherId = in.readString();
    PublishedDate = in.readString();
    ReadState = in.readInt() != 0;
    SeriesId = in.readString();
    Title = in.readString();
  }

  /**
   * Gets the full unique identifier for the comic book.
   * @return A string that represents the PublisherId, SeriesId, and the IssueCode.
   */
  public String getFullId() {

    return String.format(Locale.US, "%s-%s", getProductId(), IssueCode);
  }

  /**
   * Gets the unique identifier for the comic book.
   * @return A string that represents the PublisherId and Series Id.
   */
  public String getProductId() {

    return String.format(Locale.US, "%s%s", PublisherId, SeriesId);
  }

  /*
    Object Override(s)
   */
  @Override
  public int describeContents() { return 0; }

  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "ComicBook { Title=%s, SeriesId=%s, Issue=%d, %s, %s}",
      Title,
      SeriesId,
      IssueNumber,
      OwnedState ? "Owned" : "OnWishlist",
      ReadState ? "Read" : "Unread");
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeInt(CoverVersion);
    dest.writeString(IssueCode);
    dest.writeInt(IssueNumber);
    dest.writeByte((byte) (OwnedState ? 1 : 0));
    dest.writeInt(PrintRun);
    dest.writeString(PublisherId);
    dest.writeString(PublishedDate);
    dest.writeByte((byte) (ReadState ? 1 : 0));
    dest.writeString(SeriesId);
    dest.writeString(Title);
  }

  /*
    Public Methods
   */
  public static final Creator<ComicBook> CREATOR = new Creator<ComicBook>() {

    @Override
    public ComicBook createFromParcel(Parcel in) { return new ComicBook(in); }

    @Override
    public ComicBook[] newArray(int size) { return new ComicBook[size]; }
  };

  /**
   * Validates the properties of the comic book.
   * @return TRUE if all properties are within expected parameters, otherwise FALSE.
   */
  public boolean isValid() {

    LogUtils.debug(TAG, "++isValid()");
    if (PublisherId == null ||
      PublisherId.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID) ||
      PublisherId.length() != BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length()) {
      LogUtils.debug(TAG, "Publisher data is unexpected: %s", PublisherId);
      return false;
    }

    if (SeriesId == null ||
      SeriesId.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID) ||
      SeriesId.length() != BaseActivity.DEFAULT_COMIC_SERIES_ID.length()) {
      LogUtils.debug(TAG, "Series data is unexpected: %s", SeriesId);
      return false;
    }

    if (IssueCode == null ||
      IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) ||
      IssueCode.length() != BaseActivity.DEFAULT_ISSUE_CODE.length()) {
      LogUtils.debug(TAG, "IssueCode data is unexpected: %s", IssueCode);
      return false;
    }

    return IssueNumber >= 0 && PrintRun >= 0 && CoverVersion >= 0;
  }

  /**
   * Attempts to extract the issue number, cover variant, and print run from the issue code.
   * @param issueCode String representing the issue code.
   */
  public void parseIssueCode(String issueCode) {

    LogUtils.debug(TAG, "++parseIssueCode(%s)", issueCode);
    IssueCode = issueCode;
    if (IssueCode != null && IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length()) {
      try {
        String temp = IssueCode.substring(0, IssueCode.length() - 2);
        IssueNumber = Integer.parseInt(temp);
        temp = IssueCode.substring(IssueCode.length() - 2, IssueCode.length() - 1);
        CoverVersion = Integer.parseInt(temp);
        temp = IssueCode.substring(IssueCode.length() -1);
        PrintRun = Integer.parseInt(temp);
      } catch (Exception e) {
        LogUtils.warn(TAG, "Failed to convert issue code into values: %s", issueCode);
        IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
        IssueNumber = -1;
        CoverVersion = -1;
        PrintRun = -1;
      }
    } else {
      IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
      IssueNumber = -1;
      CoverVersion = -1;
      PrintRun = -1;
    }
  }

  /**
   * Attempts to extract the Publisher and Series identifiers from the product code.
   * @param productCode 12 character string representing the product code.
   */
  public void parseProductCode(String productCode) {

    LogUtils.debug(TAG, "++parseProductCode(%s)", productCode);
    if (productCode != null && productCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
      try {
        PublisherId = productCode.substring(0, BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
        SeriesId = productCode.substring(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
      } catch (Exception e) {
        LogUtils.warn(TAG, "Could not parse product code: %s", productCode);
        PublisherId = "";
        SeriesId = "";
      }
    }
  }
}
