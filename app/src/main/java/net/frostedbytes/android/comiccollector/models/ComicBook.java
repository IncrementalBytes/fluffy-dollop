package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.comiccollector.BaseActivity;

import java.util.Calendar;
import java.util.Locale;

public class ComicBook implements Parcelable {

    @Exclude
    public static final String ROOT = "ComicBooks";

    @Exclude
    public static final int SCHEMA_FIELDS = 11;

    /**
     * Date comic was added to user's library.
     */
    public long AddedDate;

    /**
     * Whether or not comic is owned by user.
     */
    public boolean IsOwned;

    /**
     * Unique code for issue (paired with SeriesCode).
     */
    public String IssueCode;

    /**
     * Whether or not comic is on user's wishlist.
     */
    public boolean OnWishlist;

    /**
     * Date comic was published.
     */
    public long PublishedDate;

    /**
     * Publishing house of comic.
     */
    public String Publisher;

    /**
     * Unique produce code (UPC) of comic series.
     */
    public String SeriesCode;

    /**
     * Name of series; e.g. character or story arc
     */
    public String SeriesName;

    /**
     * Title of comic; can be blank.
     */
    public String Title;

    /**
     * Date this comic instance was updated in user's library.
     */
    public long UpdatedDate;

    /**
     * Unique volume of series.
     */
    public int Volume;

    public ComicBook() {

        AddedDate = 0;
        IsOwned = false;
        IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
        OnWishlist = false;
        PublishedDate = Calendar.getInstance().getTimeInMillis();
        Publisher = "";
        SeriesCode = BaseActivity.DEFAULT_SERIES_CODE;
        SeriesName = "";
        Title = "";
        UpdatedDate = 0;
        Volume = 0;
    }

    protected ComicBook(Parcel in) {

        AddedDate = in.readLong();
        IsOwned = in.readInt() != 0;
        IssueCode = in.readString();
        OnWishlist = in.readInt() != 0;
        PublishedDate = in.readLong();
        Publisher = in.readString();
        SeriesCode = in.readString();
        SeriesName = in.readString();
        Title = in.readString();
        UpdatedDate = in.readLong();
        Volume = in.readInt();
    }

    @Exclude
    public int getIssueNumber() {

        if (IssueCode != null && IssueCode.length() > 3) {
            String temp = IssueCode.substring(0, IssueCode.length() - 2);
            return Integer.parseInt(temp);
        }

        return 0;
    }

    @Exclude
    public String getUniqueId() {

        return String.format(Locale.US, "%s-%s", SeriesCode, IssueCode);
    }

    public static final Creator<ComicBook> CREATOR = new Creator<ComicBook>() {

        @Override
        public ComicBook createFromParcel(Parcel in) { return new ComicBook(in); }

        @Override
        public ComicBook[] newArray(int size) { return new ComicBook[size]; }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public String toString() {
        return "ComicBook{" +
            "Title=" + Title +
            " (Series=" + getUniqueId() +
            ", IsOwned=" + IsOwned +
            ", OnWishlist=" + OnWishlist +
            ")}";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(AddedDate);
        dest.writeByte((byte) (IsOwned ? 1 : 0));
        dest.writeString(IssueCode);
        dest.writeByte((byte) (OnWishlist ? 1 : 0));
        dest.writeLong(PublishedDate);
        dest.writeString(Publisher);
        dest.writeString(SeriesCode);
        dest.writeString(SeriesName);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
        dest.writeInt(Volume);
    }
}
