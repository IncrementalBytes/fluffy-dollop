/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.whollynugatory.android.comiccollector.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;

@Dao
public interface SeriesDao {

  @Query("DELETE FROM series_table WHERE id == :productCode")
  void delete(String productCode);

  @Query("SELECT * from series_table WHERE id == :productCode")
  LiveData<SeriesEntity> get(String productCode);

  @Query("SELECT * from series_table ORDER BY title DESC")
  LiveData<List<SeriesEntity>> getAll();

  @Query(
    "INSERT INTO series_table (" +
      "id, " +
      "publisher_id, " +
      "series_id, " +
      "title, " +
      "volume) " +
      "VALUES (:productCode,:publisherId,:seriesId,:title,:volume)")
  void insert(String productCode, String publisherId, String seriesId, String title, int volume);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(SeriesEntity seriesEntity);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<SeriesEntity> seriesEntityList);

  @Update
  void update(SeriesEntity seriesEntity);
}
