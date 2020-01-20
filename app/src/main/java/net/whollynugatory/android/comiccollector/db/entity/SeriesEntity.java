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
package net.whollynugatory.android.comiccollector.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.BaseActivity;

@Entity(
  tableName = "series_table",
  foreignKeys = @ForeignKey(entity = PublisherEntity.class, parentColumns = "id", childColumns = "publisher_id"),
  indices = { @Index(value = {"publisher_id", "series_id"}, unique = true)})
public class SeriesEntity {

  @PrimaryKey()
  @NonNull
  @ColumnInfo(name = "id")
  @SerializedName("id")
  @Exclude
  public String Id;

  @Ignore
  public boolean IsFlagged;

  @Ignore
  @Exclude
  public List<String> OwnedIssues;

  @Ignore
  @Exclude
  public String Published;

  @NonNull
  @ColumnInfo(name = "publisher_id")
  @SerializedName("publisher_id")
  @Exclude
  public String PublisherId;

  @NonNull
  @ColumnInfo(name = "series_id")
  @SerializedName("series_id")
  @Exclude
  public String SeriesId;

  @Ignore
  public long SubmissionDate;

  @Ignore
  public String SubmittedBy;

  @NonNull
  @ColumnInfo(name = "title")
  @SerializedName("title")
  public String Title;

  @ColumnInfo(name = "volume")
  @SerializedName("volume")
  public int Volume;

  public SeriesEntity() {

    Id = BaseActivity.DEFAULT_PRODUCT_CODE;
    IsFlagged = false;
    OwnedIssues = new ArrayList<>();
    Published = "";
    PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
    SeriesId = BaseActivity.DEFAULT_SERIES_ID;
    SubmissionDate = 0;
    SubmittedBy = "";
    Title = "";
    Volume = 0;
  }

  public SeriesEntity(@NonNull String id, @NonNull String publisherId, @NonNull String seriesId, @NonNull String title, int volume) {

    Id = id;
    IsFlagged = false;
    OwnedIssues = new ArrayList<>();
    Published = "";
    PublisherId = publisherId;
    SeriesId = seriesId;
    SubmissionDate = 0;
    SubmittedBy = "";
    Title = title;
    Volume = volume;
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "Series { Title=%s, PublisherId=%s, SeriesId=%s , Volume=%d }",
      Title,
      PublisherId,
      SeriesId,
      Volume);
  }

  /*
    Public Method(s)
   */
  @Exclude
  public boolean isValid() {

    return !Id.equals(BaseActivity.DEFAULT_PRODUCT_CODE) &&
      Id.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length() &&
      !PublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) &&
      PublisherId.length() == BaseActivity.DEFAULT_PUBLISHER_ID.length() &&
      !SeriesId.equals(BaseActivity.DEFAULT_SERIES_ID) &&
      SeriesId.length() == BaseActivity.DEFAULT_SERIES_ID.length() &&
      !Title.isEmpty();
  }

  /**
   * Attempts to extract the Publisher and Series identifiers from the product code.
   * @param productCode 12 character string representing the product code.
   */
  public void parseProductCode(String productCode) {

    if (productCode != null &&
      productCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length() &&
      !productCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE)) {
      try {
        Id = productCode;
        PublisherId = productCode.substring(0, BaseActivity.DEFAULT_PUBLISHER_ID.length());
        SeriesId = productCode.substring(BaseActivity.DEFAULT_SERIES_ID.length());
      } catch (Exception e) {
        Id = BaseActivity.DEFAULT_PRODUCT_CODE;
        PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
        SeriesId = BaseActivity.DEFAULT_SERIES_ID;
      }
    }
  }
}
