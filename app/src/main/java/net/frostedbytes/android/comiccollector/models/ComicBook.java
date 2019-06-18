package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.Exclude;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.frostedbytes.android.comiccollector.BaseActivity;

import java.util.Locale;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class ComicBook implements Parcelable {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicBook";
  private static final int SCHEMA_FIELDS = 7;

  @Exclude
  public static final String ROOT = "ComicBooks";

  /**
   * Date comic was added to user's library.
   */
  public long AddedDate;

  /**
   * Cover version of comic.
   */
  @Exclude
  public int CoverVersion;

  /**
   * Unique code for issue (paired with SeriesId); populates IssueNumber, CoverVersion, and PrintRun.
   */
  @Exclude
  public String IssueCode;

  /**
   * Specific issue number.
   */
  @Exclude
  public int IssueNumber;

  /**
   * Date this comic instance was modified in user's library.
   */
  public long ModifiedDate;

  /**
   * Whether or not comic is owned by the user, otherwise it's on their wishlist.
   */
  public boolean OwnedState;

  /**
   * Print run number.
   */
  @Exclude
  public int PrintRun;

  /**
   * Date comic was published.
   */
  public long PublishedDate;

  /**
   * Unique identifier for publisher of comic.
   */
  @Exclude
  public String PublisherId;

  /**
   * Whether or not comic is read by the user, otherwise it's unread.
   */
  public boolean ReadState;

  /**
   * Unique identifier for the series this comic is published under.
   */
  @Exclude
  public String SeriesId;

  /**
   * Title for comic; can be blank.
   */
  public String Title;

  public ComicBook() {

    AddedDate = 0;
    CoverVersion = -1;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    IssueNumber = -1;
    ModifiedDate = 0;
    OwnedState = false;
    PrintRun = -1;
    PublishedDate = 0;
    PublisherId = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
    ReadState = false;
    SeriesId = BaseActivity.DEFAULT_COMIC_SERIES_ID;
    Title = "";
  }

  public ComicBook(ComicBook comicBook) {

    AddedDate = comicBook.AddedDate;
    CoverVersion = comicBook.CoverVersion;
    IssueCode = comicBook.IssueCode;
    IssueNumber = comicBook.IssueNumber;
    ModifiedDate = comicBook.ModifiedDate;
    OwnedState = comicBook.OwnedState;
    PrintRun = comicBook.PrintRun;
    PublisherId = comicBook.PublisherId;
    PublishedDate = comicBook.PublishedDate;
    ReadState = comicBook.ReadState;
    SeriesId = comicBook.SeriesId;
    Title = comicBook.Title;
  }

  protected ComicBook(Parcel in) {

    AddedDate = in.readLong();
    CoverVersion = in.readInt();
    IssueCode = in.readString();
    IssueNumber = in.readInt();
    ModifiedDate = in.readLong();
    OwnedState = in.readInt() != 0;
    PrintRun = in.readInt();
    PublisherId = in.readString();
    PublishedDate = in.readLong();
    ReadState = in.readInt() != 0;
    SeriesId = in.readString();
    Title = in.readString();
  }

  /**
   * Gets the full unique identifier for the comic book.
   * @return A string that represents the PublisherId, SeriesId, and the IssueCode.
   */
  @Exclude
  public String getFullId() {

    return String.format(Locale.US, "%s-%s", getProductId(), IssueCode);
  }

  /**
   * Gets the unique identifier for the comic book.
   * @return A string that reprents the PublisherId and Series Id.
   */
  @Exclude
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

    dest.writeLong(AddedDate);
    dest.writeInt(CoverVersion);
    dest.writeString(IssueCode);
    dest.writeInt(IssueNumber);
    dest.writeLong(ModifiedDate);
    dest.writeByte((byte) (OwnedState ? 1 : 0));
    dest.writeInt(PrintRun);
    dest.writeString(PublisherId);
    dest.writeLong(PublishedDate);
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
   * Looks for local copy of library in the user's application data.
   * @param fileDir Path to local library.
   * @return List of comic books parsed from local library. An empty list if library not found.
   */
  public static ArrayList<ComicBook> readLocalLibrary(File fileDir) {

    LogUtils.debug(TAG, "++readLocalLibrary()");
    String parsableString;
    String resourcePath = BaseActivity.DEFAULT_LIBRARY_FILE;
    File file = new File(fileDir, resourcePath);
    LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
    ArrayList<ComicBook> comicBooks = new ArrayList<>();
    try {
      if (file.exists() && file.canRead()) {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        while ((parsableString = bufferedReader.readLine()) != null) { //process line
          if (parsableString.startsWith("--")) { // comment line; ignore
            continue;
          }

          List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
          if (elements.size() != ComicBook.SCHEMA_FIELDS) {
            LogUtils.debug(
              TAG,
              "Local library schema mismatch. Got: %d Expected: %d",
              elements.size(),
              ComicBook.SCHEMA_FIELDS);
            continue;
          }

          ComicBook comicBook = ComicBook.fromList(elements);
          if (comicBook != null) {
            comicBooks.add(comicBook);
            LogUtils.debug(TAG, "Adding %s to collection.", comicBook.toString());
          } else {
            LogUtils.warn(TAG, "Could not create ComicBook from: %s", parsableString);
          }
        }
      } else {
        LogUtils.debug(TAG, "%s does not exist yet.", resourcePath);
      }
    } catch (Exception e) {
      LogUtils.warn(TAG, "Exception when reading local library data.");
      Crashlytics.logException(e);
    }

    return comicBooks;
  }

  /**
   * Validates the properties of the comic book.
   * @return TRUE if all properties are within expected parameters, otherwise FALSE.
   */
  @Exclude
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

  /**
   * Provides a single line of text representing the comic book, delimited by the '|' character.
   * @return Single line text representing this comic book.
   */
  public String writeLine() {

    return String.format(
      Locale.US,
      "%s%s-%s|%s|%s|%s|%s|%s|%s\r\n",
      PublisherId,
      SeriesId,
      IssueCode,
      String.valueOf(PublishedDate),
      Title,
      String.valueOf(OwnedState),
      String.valueOf(ReadState),
      String.valueOf(AddedDate),
      String.valueOf(ModifiedDate));
  }

  /*
    Private Method(s)
   */
  private static ComicBook fromList(List<String> elements) {

    ComicBook comicBook = new ComicBook();
    try {
      comicBook.parseProductCode(elements.remove(0));
      comicBook.parseIssueCode(elements.remove(0));
      comicBook.PublishedDate = Long.parseLong(elements.remove(0));
      comicBook.Title = elements.remove(0);
      comicBook.OwnedState = Boolean.parseBoolean(elements.remove(0));
      comicBook.ReadState = Boolean.parseBoolean(elements.remove(0));
      comicBook.AddedDate = Long.parseLong(elements.remove(0));
      comicBook.ModifiedDate = Long.parseLong(elements.remove(0));
    } catch (Exception ex) {
      return null;
    }

    return comicBook;
  }
}
