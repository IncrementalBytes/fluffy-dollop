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
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;

@Dao
public interface ComicBookDao {

  @Query("DELETE from comic_book_table WHERE Id == :comicBookId")
  void deleteById(String comicBookId);

  @Query("SELECT * from comic_book_table")
  LiveData<List<ComicBookEntity>> exportable();

  @Query("SELECT * from comic_book_table WHERE Id == :comicBookId")
  LiveData<ComicBookEntity> find(String comicBookId);

  @Query("SELECT * FROM comic_book_table ORDER BY added_date DESC LIMIT 50")
  LiveData<List<ComicBookEntity>> getAllByRecent();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(ComicBookEntity comicBookEntity);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<ComicBookEntity> comicBookEntityList);

  @Update
  void update(ComicBookEntity comicBookEntity);
}
