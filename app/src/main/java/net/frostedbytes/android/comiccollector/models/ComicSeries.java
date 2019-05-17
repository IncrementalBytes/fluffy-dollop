package net.frostedbytes.android.comiccollector.models;

import android.os.Parcel;
import android.os.Parcelable;

import net.frostedbytes.android.comiccollector.BaseActivity;

public class ComicSeries implements Parcelable {

    /**
     * Unique UPC code for series
     */
    public String Code;

    /**
     * Name of series; e.g. character or story arc
     */
    public String Name;

    /**
     * Publishing house of comic.
     */
    public String Publisher;

    /**
     * Unique volume of series.
     */
    public int Volume;

    public ComicSeries() {

        Code = BaseActivity.DEFAULT_SERIES_CODE;
        Name = "";
        Publisher = "";
        Volume = 0;
    }

    protected ComicSeries(Parcel in) {

        Code = in.readString();
        Name = in.readString();
        Publisher = in.readString();
        Volume = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(Code);
        dest.writeString(Name);
        dest.writeString(Publisher);
        dest.writeInt(Volume);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<ComicSeries> CREATOR = new Creator<ComicSeries>() {

        @Override
        public ComicSeries createFromParcel(Parcel in) { return new ComicSeries(in); }

        @Override
        public ComicSeries[] newArray(int size) { return new ComicSeries[size]; }
    };
}
