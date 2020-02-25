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
import java.util.Locale;
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@DatabaseView(
  "SELECT Publishers.id AS PublisherId, " +
    "Publishers.publisher_code AS PublisherCode, " +
    "Series.id AS SeriesId, " +
    "Series.series_code AS SeriesCode, " +
    "Series.name AS SeriesTitle, " +
    "Series.volume AS Volume, " +
    "Publishers.Name AS Publisher, " +
    "Series.added_date AS AddedDate, " +
    "Series.updated_date AS UpdatedDate " +
    "FROM series_table AS Series " +
    "INNER JOIN publisher_table AS Publishers ON Publishers.id = Series.publisher_id")
public class SeriesDetails implements Serializable {

  public String PublisherId;
  public String PublisherCode;
  public String SeriesId;
  public String SeriesCode;
  public String SeriesTitle;
  public int Volume;
  public String Publisher;
  public long AddedDate;
  public long UpdatedDate;

  @Ignore
  public boolean PublisherChanged;

  @Ignore
  public boolean SeriesChanged;

  public SeriesDetails() {

    PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
    PublisherCode = BaseActivity.DEFAULT_PUBLISHER_CODE;
    SeriesId = BaseActivity.DEFAULT_SERIES_ID;
    SeriesCode = BaseActivity.DEFAULT_SERIES_CODE;
    SeriesTitle = "";
    Volume = -1;
    Publisher = "";
    AddedDate = 0;
    UpdatedDate = 0;

    PublisherChanged = false;
    SeriesChanged = false;
  }

  @Ignore
  public String getProductCode() {

    return String.format(Locale.US, "%s%s", PublisherCode, SeriesCode);
  }

  @Ignore
  public boolean isValid() {

    return !PublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) &&
      PublisherId.length() == BaseActivity.DEFAULT_PUBLISHER_ID.length() &&
      !Publisher.isEmpty() &&
      !SeriesId.equals(BaseActivity.DEFAULT_SERIES_ID) &&
      SeriesId.length() == BaseActivity.DEFAULT_SERIES_ID.length() &&
      !SeriesTitle.isEmpty();
  }

  @Ignore
  public PublisherEntity toPublisherEntity() {

    PublisherEntity publisherEntity = new PublisherEntity();
    publisherEntity.Id = PublisherId;
    publisherEntity.Name = Publisher;
    publisherEntity.PublisherCode = PublisherCode;
    return publisherEntity;
  }

  @Ignore
  public SeriesEntity toSeriesEntity() {
    SeriesEntity seriesEntity = new SeriesEntity();
    seriesEntity.Id = SeriesId;
    seriesEntity.Name = SeriesTitle;
    seriesEntity.SeriesCode = SeriesCode;
    return seriesEntity;
  }
}
