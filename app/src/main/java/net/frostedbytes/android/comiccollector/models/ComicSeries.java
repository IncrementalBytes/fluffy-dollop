package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class ComicSeries implements Parcelable {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicSeries";

  @Exclude
  public static final String ROOT = "ComicSeries";

  /**
   * Date when comic series was added to collection.
   */
  public long AddedDate;

  /**
   * Collection of comic books user added to this series.
   */
  @Exclude
  public ArrayList<ComicBook> ComicBooks;

  /**
   * Unique identifier for series.
   */
  @Exclude
  public String Id;

  /**
   * The value indicating whether or not this comic series has been flagged for review.
   */
  public boolean IsFlagged;

  /**
   * Date when comic series details were modified.
   */
  public long ModifiedDate;

  /**
   * Unique identifier for publisher of series.
   */
  @Exclude
  public String PublisherId;

  /**
   * Name of series; e.g. character or story arc
   */
  public String SeriesName;

  /**
   * Unique volume of series.
   */
  public int Volume;

  public ComicSeries() {

    AddedDate = 0;
    ComicBooks = new ArrayList<>();
    Id = BaseActivity.DEFAULT_COMIC_SERIES_ID;
    IsFlagged = false;
    ModifiedDate = 0;
    SeriesName = "";
    PublisherId = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
    Volume = 0;
  }

  public ComicSeries(ComicSeries existing) {

    AddedDate = existing.AddedDate;
    ComicBooks = new ArrayList<>(existing.ComicBooks);
    Id = existing.Id;
    IsFlagged = existing.IsFlagged;
    ModifiedDate = existing.ModifiedDate;
    SeriesName = existing.SeriesName;
    PublisherId = existing.PublisherId;
    Volume = existing.Volume;
  }

  protected ComicSeries(Parcel in) {

    AddedDate = in.readLong();
    ComicBooks = in.readArrayList(ComicBook.class.getClassLoader());
    Id = in.readString();
    IsFlagged = in.readInt() != 0;
    ModifiedDate = in.readLong();
    SeriesName = in.readString();
    PublisherId = in.readString();
    Volume = in.readInt();
  }

  /**
   * Gets the unique product identifier for this comic series.
   * @return The unique combination of the publisher and series identifiers.
   */
  @Exclude
  public String getProductId() {

    return String.format(Locale.US, "%s%s", PublisherId, Id);
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
      "ComicSeries { Name=%s, PublisherId=%s, SeriesId=%s, Volume=%d, %s }",
      SeriesName,
      PublisherId,
      Id,
      Volume,
      IsFlagged ? "Needs Review" : "Accepted");
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeLong(AddedDate);
    dest.writeList(ComicBooks);
    dest.writeString(Id);
    dest.writeInt(IsFlagged ? 1 : 0);
    dest.writeLong(ModifiedDate);
    dest.writeString(SeriesName);
    dest.writeString(PublisherId);
    dest.writeInt(Volume);
  }

  /*
    Public Method(s)
   */
  public static final Creator<ComicSeries> CREATOR = new Creator<ComicSeries>() {

    @Override
    public ComicSeries createFromParcel(Parcel in) { return new ComicSeries(in); }

    @Override
    public ComicSeries[] newArray(int size) { return new ComicSeries[size]; }
  };

  /**
   * Attempts to extract the Publisher and Series identifiers from the product code.
   * @param productCode 12 character string representing the product code.
   */
  public void parseProductCode(String productCode) {

    LogUtils.debug(TAG, "++parseProductCode(%s)", productCode);
    if (productCode != null && productCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
      try {
        PublisherId = productCode.substring(0, BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
        Id = productCode.substring(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length());
      } catch (Exception e) {
        LogUtils.warn(TAG, "Could not parse product code: %s", productCode);
        PublisherId = "";
        Id = "";
      }
    }
  }

  /**
   * Validates the properties of the comic series.
   * @return TRUE if all properties are within expected parameters, otherwise FALSE.
   */
  @Exclude
  public boolean isValid() {

    LogUtils.debug(TAG, "++isValid()");
    if (PublisherId == null ||
      PublisherId.length() != BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length() ||
      PublisherId.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID)) {
      LogUtils.debug(TAG, "Publisher data is unexpected: %s", PublisherId);
      return false;
    }

    if (Id == null || Id.length() != BaseActivity.DEFAULT_COMIC_SERIES_ID.length() || Id.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID)) {
      LogUtils.debug(TAG, "Series data is unexpected: %s", Id);
      return false;
    }

    return SeriesName != null && SeriesName.length() != 0;
  }
}
