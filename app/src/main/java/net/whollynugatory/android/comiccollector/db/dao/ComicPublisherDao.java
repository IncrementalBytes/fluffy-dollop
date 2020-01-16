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
import net.whollynugatory.android.comiccollector.db.entity.ComicPublisher;

@Dao
public interface ComicPublisherDao {

  @Query("DELETE FROM comic_publisher_table WHERE id == :publisherId")
  void delete(String publisherId);

  @Query("SELECT * from comic_publisher_table WHERE id == :publisherId")
  LiveData<ComicPublisher> get(String publisherId);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(ComicPublisher publisher);
}
