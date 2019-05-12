package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.comiccollector.BaseActivity;

public class ComicBook implements Parcelable {

    @Exclude
    public static final String ROOT = "ComicBooks";
    public long AddedDate;
    public boolean IsOwned;
    public boolean OnWishlist;
    public String ProductCode;
    public String Title;
    public long UpdatedDate;

    public ComicBook() {

        this.AddedDate = 0;
        this.IsOwned = false;
        this.OnWishlist = false;
        this.ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
        this.Title = "";
        this.UpdatedDate = 0;
    }

    public ComicBook(ComicBook comicBook) {

        this.AddedDate = comicBook.AddedDate;
        this.IsOwned = comicBook.IsOwned;
        this.OnWishlist = comicBook.OnWishlist;
        this.ProductCode = comicBook.ProductCode;
        this.Title = comicBook.Title;
        this.UpdatedDate = comicBook.UpdatedDate;
    }

    protected ComicBook(Parcel in) {

        AddedDate = in.readLong();
        IsOwned = in.readInt() != 0;
        OnWishlist = in.readInt() != 0;
        ProductCode = in.readString();
        Title = in.readString();
        UpdatedDate = in.readLong();
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
        dest.writeByte((byte) (OnWishlist ? 1 : 0));
        dest.writeString(ProductCode);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
    }
}
