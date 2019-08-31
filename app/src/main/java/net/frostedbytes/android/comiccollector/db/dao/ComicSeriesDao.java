package net.frostedbytes.android.comiccollector.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import net.frostedbytes.android.comiccollector.db.entity.ComicSeries;
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;

@Dao
public interface ComicSeriesDao {

  @Query("DELETE FROM comic_series_table WHERE id == :productCode")
  void delete(String productCode);

  @Query("SELECT * from ComicSeriesDetails WHERE id == :productCode")
  LiveData<ComicSeriesDetails> get(String productCode);

  @Query("SELECT * from ComicSeriesDetails ORDER BY title DESC")
  LiveData<List<ComicSeriesDetails>> getAll();

  @Query(
    "INSERT INTO comic_series_table (" +
      "id, " +
      "publisher_id, " +
      "series_id, " +
      "title, " +
      "volume) " +
      "VALUES (:productCode,:publisherId,:seriesId,:title,:volume)")
  void insert(String productCode, String publisherId, String seriesId, String title, int volume);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(ComicSeries comicSeries);
}
