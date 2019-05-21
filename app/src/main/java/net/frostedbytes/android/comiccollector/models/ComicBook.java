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
    public static final int SCHEMA_FIELDS = 10;

    /**
     * Date comic was added to user's library.
     */
    public long AddedDate;

    /**
     * Unique code for issue (paired with SeriesCode).
     */
    public String IssueCode;

    /**
     * Whether or not comic is owned by the user, otherwise it's on their wishlist.
     */
    public boolean OwnedState;

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
        IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
        OwnedState = false;
        PublishedDate = Calendar.getInstance().getTimeInMillis();
        Publisher = "";
        SeriesCode = BaseActivity.DEFAULT_SERIES_CODE;
        SeriesName = "";
        Title = "";
        UpdatedDate = 0;
        Volume = -1;
    }

    public ComicBook(ComicBook comicBook) {

        AddedDate = comicBook.AddedDate;
        IssueCode = comicBook.IssueCode;
        OwnedState = comicBook.OwnedState;
        PublishedDate = comicBook.PublishedDate;
        Publisher = comicBook.Publisher;
        SeriesCode = comicBook.SeriesCode;
        SeriesName = comicBook.SeriesName;
        Title = comicBook.Title;
        UpdatedDate = comicBook.UpdatedDate;
        Volume = comicBook.Volume;
    }

    protected ComicBook(Parcel in) {

        AddedDate = in.readLong();
        IssueCode = in.readString();
        OwnedState = in.readInt() != 0;
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

        return String.format("ComicBook { Title=%s, Series=%s, %s }", Title, getUniqueId(), OwnedState ? "Owned=true" : "OnWishlist=true");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(AddedDate);
        dest.writeString(IssueCode);
        dest.writeByte((byte) (OwnedState ? 1 : 0));
        dest.writeLong(PublishedDate);
        dest.writeString(Publisher);
        dest.writeString(SeriesCode);
        dest.writeString(SeriesName);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
        dest.writeInt(Volume);
    }

    public boolean IsValid() {

        if (IssueCode.isEmpty() || IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE)) {
            return false;
        }

        if (Publisher.isEmpty()) {
            return false;
        }

        if (SeriesCode.isEmpty() || SeriesCode.equals(BaseActivity.DEFAULT_SERIES_CODE)) {
            return false;
        }

        if (SeriesName.isEmpty()) {
            return false;
        }

        return Volume >= 0;
    }
}