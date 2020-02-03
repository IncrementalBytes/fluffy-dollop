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

package net.whollynugatory.android.comiccollector.db.views;

import androidx.room.DatabaseView;
import androidx.room.Ignore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@DatabaseView(
  "SELECT Series.id AS Id, " +
    "Series.name AS Title, " +
    "Series.volume AS Volume, " +
    "Publishers.name AS PublisherName " +
  "FROM series_table AS Series " +
  "INNER JOIN publisher_table As Publishers ON Series.publisher_id = Publishers.id " +
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
