/*
 * Copyright 2020 Ryan Ward
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

  @Query("DELETE FROM series_table WHERE series_code == :seriesCode")
  void delete(String seriesCode);

  @Query("SELECT * from series_table WHERE publisher_id == :publisherId AND series_code == :seriesCode")
  LiveData<SeriesEntity> get(String publisherId, String seriesCode);

  @Query("SELECT * from series_table ORDER BY name DESC")
  LiveData<List<SeriesEntity>> getAll();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(SeriesEntity seriesEntity);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<SeriesEntity> seriesEntityList);

  @Update
  void update(SeriesEntity seriesEntity);
}
