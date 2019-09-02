package net.frostedbytes.android.comiccollector.db.views;

import androidx.room.DatabaseView;
import androidx.room.Ignore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@DatabaseView(
  "SELECT Series.id AS Id, " +
    "Series.title AS Title, " +
    "Series.volume AS Volume, " +
    "Publishers.name AS PublisherName " +
  "FROM comic_series_table AS Series " +
  "INNER JOIN comic_publisher_table As Publishers ON Series.publisher_id = Publishers.id " +
  "WHERE Series.id != -1")
public class ComicSeriesDetails implements Serializable {

  public String Id;
  public String Title;
  public int Volume;
  public String PublisherName;

  @Ignore
  public List<Integer> OwnedIssues;

  @Ignore
  public List<Integer> Published;

  public ComicSeriesDetails() {

    OwnedIssues = new ArrayList<>();
    Published = new ArrayList<>();
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "ComicSeries { Name=%s, Publisher=%s, ProductCode=%s, Volume=%d }",
      Title,
      PublisherName,
      Id,
      Volume);
  }
}
