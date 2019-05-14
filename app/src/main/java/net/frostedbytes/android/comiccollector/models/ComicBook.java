package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.comiccollector.BaseActivity;

public class ComicBook implements Parcelable {

    @Exclude
    public static final String ROOT = "ComicBooks";

    @Exclude
    public static final int SCHEMA_FIELDS = 12;

    /**
     * Date comic was added to user's library.
     */
    public long AddedDate;

    /**
     * Whether or not comic is owned by user.
     */
    public boolean IsOwned;

    /**
     * Issue number of comic (can be in conjunction with Volume).
     */
    public int Issue;

    /**
     * Unique code for issue.
     */
    public String IssueCode;

    /**
     * Whether or not comic is on user's wishlist.
     */
    public boolean OnWishlist;

    /**
     * Unique produce code (UPC) of comic.
     */
    @Exclude
    public String ProductCode;

    /**
     * Date comic was published.
     */
    public long PublishedDate;

    /**
     * Publishing house of comic.
     */
    public String Publisher;

    /**
     * Series name of issue.
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
     * Unique volume comic is include in (may be 0).
     */
    public int Volume;

    public ComicBook() {

        this.AddedDate = 0;
        this.IsOwned = false;
        this.Issue = 0;
        this.IssueCode = "";
        this.OnWishlist = false;
        this.ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
        this.PublishedDate = 0;
        this.Publisher = "";
        this.SeriesName = "";
        this.Title = "";
        this.UpdatedDate = 0;
        this.Volume = 0;
    }

    public ComicBook(ComicBook comicBook) {

        this.AddedDate = comicBook.AddedDate;
        this.IsOwned = comicBook.IsOwned;
        this.Issue = comicBook.Issue;
        this.IssueCode = comicBook.IssueCode;
        this.OnWishlist = comicBook.OnWishlist;
        this.ProductCode = comicBook.ProductCode;
        this.PublishedDate = comicBook.PublishedDate;
        this.Publisher = comicBook.Publisher;
        this.SeriesName = comicBook.SeriesName;
        this.Title = comicBook.Title;
        this.UpdatedDate = comicBook.UpdatedDate;
        this.Volume = comicBook.Volume;
    }

    protected ComicBook(Parcel in) {

        AddedDate = in.readLong();
        IsOwned = in.readInt() != 0;
        Issue = in.readInt();
        IssueCode = in.readString();
        OnWishlist = in.readInt() != 0;
        ProductCode = in.readString();
        PublishedDate = in.readLong();
        Publisher = in.readString();
        SeriesName = in.readString();
        Title = in.readString();
        UpdatedDate = in.readLong();
        Volume = in.readInt();
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
            " (ProductCode=" + ProductCode +
            ", IsOwned=" + IsOwned +
            ", OnWishlist=" + OnWishlist +
            ")}";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(AddedDate);
        dest.writeByte((byte) (IsOwned ? 1 : 0));
        dest.writeInt(Issue);
        dest.writeString(IssueCode);
        dest.writeByte((byte) (OnWishlist ? 1 : 0));
        dest.writeString(ProductCode);
        dest.writeLong(PublishedDate);
        dest.writeString(Publisher);
        dest.writeString(SeriesName);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
        dest.writeInt(Volume);
    }
}
