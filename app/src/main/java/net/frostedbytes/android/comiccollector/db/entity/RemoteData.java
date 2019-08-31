package net.frostedbytes.android.comiccollector.db.entity;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

public class RemoteData {

  @SerializedName("publishers")
  public ArrayList<ComicPublisher> ComicPublishers;

  @SerializedName("series")
  public ArrayList<ComicSeries> ComicSeries;

  public RemoteData() {

    ComicPublishers = new ArrayList<>();
    ComicSeries = new ArrayList<>();
  }
}
