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
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;

@Dao
public interface ComicDetailsDao {

  @Query("SELECT * from ComicDetails WHERE PublisherCode == :publisherCode AND SeriesCode == :seriesCode AND IssueCode == :issueCode")
  LiveData<ComicDetails> get(String publisherCode, String seriesCode, String issueCode);

  @Query("SELECT * from ComicDetails WHERE SeriesId == :seriesId ORDER BY UpdatedDate DESC")
  LiveData<List<ComicDetails>> getBySeriesId(String seriesId);

  @Query("SELECT * FROM ComicDetails ORDER BY UpdatedDate DESC LIMIT 50")
  LiveData<List<ComicDetails>> getRecent();
}
