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
import androidx.room.Query;
import java.util.List;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;

@Dao
public interface SeriesDetailsDao {

  @Query("SELECT * from SeriesDetails WHERE PublisherCode == :publisherCode AND SeriesCode == :seriesCode")
  LiveData<SeriesDetails> get(String publisherCode, String seriesCode);

  @Query("SELECT * from SeriesDetails")
  LiveData<List<SeriesDetails>> getAll();

  @Query("SELECT * FROM SeriesDetails WHERE PublisherCode == :publisherCode")
  LiveData<List<SeriesDetails>> getAllByPublisher(String publisherCode);
}
