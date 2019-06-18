package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.Exclude;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;

public class ComicPublisher implements Parcelable {

  @Exclude
  public static final String ROOT = "ComicPublishers";

  /**
   * Date when comic series was added to collection.
   */
  public long AddedDate;

  /**
   * Unique identifier for publisher; should match the UPC.
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
   * Name of series; e.g. character or story arc
   */
  public String Name;

  public ComicPublisher() {

    AddedDate = 0;
    Id = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
    IsFlagged = false;
    ModifiedDate = 0;
    Name = "";
  }

  protected ComicPublisher(Parcel in) {

    AddedDate = in.readLong();
    Id = in.readString();
    IsFlagged = in.readInt() != 0;
    ModifiedDate = in.readLong();
    Name = in.readString();
  }

  /*
    Object Override(s)
   */
  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "ComicPublisher { Name=%s, Id=%s, %s }",
      Name,
      Id,
      IsFlagged ? "Needs Review" : "Accepted");
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeLong(AddedDate);
    dest.writeString(Id);
    dest.writeInt(IsFlagged ? 1 : 0);
    dest.writeLong(ModifiedDate);
    dest.writeString(Name);
  }

  /*
    Public Method(s)
   */
  public static final Creator<ComicPublisher> CREATOR = new Creator<ComicPublisher>() {

    @Override
    public ComicPublisher createFromParcel(Parcel in) {
      return new ComicPublisher(in);
    }

    @Override
    public ComicPublisher[] newArray(int size) {
      return new ComicPublisher[size];
    }
  };
}
