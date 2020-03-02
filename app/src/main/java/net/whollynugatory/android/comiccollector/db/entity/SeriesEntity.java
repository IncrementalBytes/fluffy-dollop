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
import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(
  tableName = "series_table",
  foreignKeys = @ForeignKey(entity = PublisherEntity.class, parentColumns = "id", childColumns = "publisher_id"),
  indices = {
    @Index(value = "id"),
    @Index(value = "publisher_id")
  })
public class SeriesEntity implements Serializable {

  @Ignore
  public static final String ROOT = "Series";

  @PrimaryKey()
  @NonNull
  @ColumnInfo(name = "id")
  @SerializedName("id")
  public String Id;

  @NonNull
  @ColumnInfo(name = "series_code")
  @SerializedName("series_code")
  public String SeriesCode;

  @NonNull
  @ColumnInfo(name = "publisher_id")
  @SerializedName("publisher_id")
  public String PublisherId;

  @NonNull
  @ColumnInfo(name = "name")
  @SerializedName("name")
  public String Name;

  @ColumnInfo(name = "volume")
  @SerializedName("volume")
  public int Volume;

  @ColumnInfo(name = "active")
  @SerializedName("active")
  public boolean IsActive;

  @ColumnInfo(name = "added_date")
  @SerializedName("added_date")
  public transient long AddedDate;

  @ColumnInfo(name = "updated_date")
  @SerializedName("updated_date")
  public transient long UpdatedDate;

  @Ignore
  @SerializedName("needs_review")
  public boolean NeedsReview;

  @Ignore
  @SerializedName("submission_date")
  public long SubmissionDate;

  @Ignore
  @SerializedName("submitted_by")
  public String SubmittedBy;

  public SeriesEntity() {

    Id = UUID.randomUUID().toString();
    SeriesCode = BaseActivity.DEFAULT_SERIES_CODE;
    PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;

    Name = "";
    Volume = 0;
    AddedDate = UpdatedDate = Calendar.getInstance().getTimeInMillis();

    IsActive = true;
    NeedsReview = false;
    SubmissionDate = 0;
    SubmittedBy = "";
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "Series { Name=%s, Code=%s , Volume=%d }",
      Name,
      SeriesCode,
      Volume);
  }

  /*
    Public Method(s)
   */
  @Exclude
  public boolean isValid() {

    return !SeriesCode.equals(BaseActivity.DEFAULT_SERIES_CODE) &&
      SeriesCode.length() == BaseActivity.DEFAULT_SERIES_CODE.length() &&
      !Name.isEmpty() &&
      Volume > 0;
  }

  public static String getSeriesCode(String productCode) {

    if (productCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
      return productCode.substring(BaseActivity.DEFAULT_SERIES_CODE.length());
    }

    return BaseActivity.DEFAULT_SERIES_CODE;
  }
}
