package net.frostedbytes.android.comiccollector.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import net.frostedbytes.android.comiccollector.db.entity.ComicPublisher;

@Dao
public interface ComicPublisherDao {

  @Query("DELETE FROM comic_publisher_table WHERE id == :publisherId")
  void delete(String publisherId);

  @Query("SELECT * from comic_publisher_table WHERE id == :publisherId")
  LiveData<ComicPublisher> get(String publisherId);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(ComicPublisher publisher);
}
