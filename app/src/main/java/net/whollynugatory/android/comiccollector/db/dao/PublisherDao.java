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
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;

@Dao
public interface PublisherDao {

  @Query("DELETE FROM publisher_table WHERE publisher_code == :publisherCode")
  void delete(String publisherCode);

  @Query("SELECT DISTINCT * from publisher_table WHERE id == :publisherId")
  LiveData<PublisherEntity> getById(String publisherId);

  @Query("SELECT * from publisher_table ORDER BY name DESC")
  LiveData<List<PublisherEntity>> getAll();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(PublisherEntity publisherEntity);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<PublisherEntity> publisherEntityList);

  @Update
  void update(PublisherEntity publisherEntity);
}
