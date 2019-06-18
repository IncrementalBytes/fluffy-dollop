package net.frostedbytes.android.comiccollector.models;

import android.content.res.AssetManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.Exclude;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class ComicSeries implements Parcelable {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicSeries";
  private static final int SCHEMA_FIELDS = 7;

  @Exclude
  public static final String ROOT = "ComicSeries";

  /**
   * Date when comic series was added to collection.
   */
  public long AddedDate;

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
    Id = BaseActivity.DEFAULT_COMIC_SERIES_ID;
    IsFlagged = false;
    ModifiedDate = 0;
    SeriesName = "";
    PublisherId = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
    Volume = 0;
  }

  protected ComicSeries(Parcel in) {

    AddedDate = in.readLong();
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
  public String getId() {

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
   * Attempts to read the static asset file for Comic Series. This is usually only done if local and network queries failed.
   * @param assetManager Current instance of the asset manager (to access comic series file).
   * @return Collection of Comic Series.
   */
  public static HashMap<String, ComicSeries> parseComicSeriesAssetFile(AssetManager assetManager) {

    LogUtils.debug(TAG, "++parseComicSeriesAssetFile(AssetManager)");
    HashMap<String, ComicSeries> comicSeries = new HashMap<>();
    String parsableString;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(assetManager.open("ComicSeries.txt"))));
      while ((parsableString = reader.readLine()) != null) { //process line
        if (parsableString.startsWith("--")) { // comment line; ignore
          continue;
        }

        List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
        ComicSeries series = ComicSeries.fromList(elements);
        if (series != null) {
          if (!comicSeries.containsKey(series.Id)) {
            comicSeries.put(series.Id, series);
          }
        }
      }
    } catch (IOException e) {
      LogUtils.error(TAG, "Exception reading asset file: %s", e.getMessage());
      Crashlytics.logException(e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          LogUtils.warn(TAG, "Unable to close resource on exit: %s", e.getMessage());
        }
      }
    }

    return comicSeries;
  }

  /**
   * Attempts to read the local copy of comic series. If a failure is encountered, an empty collection is returned.
   * @param fileDir File path of local comic series data.
   * @return Populated collection of comic series.
   */
  public static HashMap<String, ComicSeries> readLocalComicSeries(File fileDir) {

    LogUtils.debug(TAG, "++readLocalComicSeries(%s)", fileDir.getName());
    String parsableString;
    String resourcePath = BaseActivity.DEFAULT_COMIC_SERIES_FILE;
    File file = new File(fileDir, resourcePath);
    LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
    HashMap<String, ComicSeries> comicSeries = new HashMap<>();
    try {
      if (file.exists() && file.canRead()) {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        while ((parsableString = bufferedReader.readLine()) != null) { //process line
          if (parsableString.startsWith("--")) { // comment line; ignore
            continue;
          }

          List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
          if (elements.size() != ComicSeries.SCHEMA_FIELDS) {
            LogUtils.debug(
              TAG,
              "Local comic series schema mismatch. Got: %d Expected: %d",
              elements.size(),
              ComicSeries.SCHEMA_FIELDS);
            continue;
          }

          ComicSeries series = ComicSeries.fromList(elements);
          if (series != null) {
            if (!comicSeries.containsKey(series.Id)) {
              comicSeries.put(series.getId(), series);
              LogUtils.debug(TAG, "Adding %s to collection.", series.toString());
            }
          } else {
            LogUtils.warn(TAG, "Could not create ComicSeries from: %s", parsableString);
          }
        }
      } else {
        LogUtils.debug(TAG, "%s does not exist yet.", resourcePath);
      }
    } catch (Exception e) {
      LogUtils.warn(TAG, "Exception when reading local comic series data.");
      Crashlytics.logException(e);
    }

    return comicSeries;
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

  /**
   * Provides a single line of text representing the comic series, delimited by the '|' character.
   * @return Single line text representing this comic series.
   */
  @Exclude
  public String writeLine() {

    return String.format(
      Locale.US,
      "%s|%s|%s|%s|%s|%s|%s\r\n",
      PublisherId,
      Id,
      SeriesName,
      String.valueOf(Volume),
      String.valueOf(AddedDate),
      String.valueOf(ModifiedDate),
      String.valueOf(IsFlagged));
  }

  /*
    Private Method(s)
   */
  private static ComicSeries fromList(List<String> elements) {

    ComicSeries comicSeries = new ComicSeries();
    try {
      comicSeries.PublisherId = elements.remove(0);
      comicSeries.Id = elements.remove(0);
      comicSeries.SeriesName = elements.remove(0);
      comicSeries.Volume = Integer.parseInt(elements.remove(0));
      comicSeries.AddedDate = Long.parseLong(elements.remove(0));
      comicSeries.ModifiedDate = Long.parseLong(elements.remove(0));
      comicSeries.IsFlagged = Boolean.parseBoolean(elements.remove(0));
    } catch (Exception ex) {
      return null;
    }

    return comicSeries;
  }
}
