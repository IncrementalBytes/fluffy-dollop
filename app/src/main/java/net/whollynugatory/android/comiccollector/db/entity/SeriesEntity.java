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
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(
  tableName = "series_table",
  indices = { @Index(value = {"id"})})
public class SeriesEntity implements Serializable {

  @Ignore
  public static final String ROOT = "Series";

  /**
   * Unique identifier for series (also known as the productCode). A combination of the publisher identifier and series identifier.
   **/
  @PrimaryKey()
  @NonNull
  @ColumnInfo(name = "id")
  @SerializedName("id")
  @Exclude
  public String Id;

  @NonNull
  @ColumnInfo(name = "publisher")
  @SerializedName("publisher")
  @Exclude
  public String Publisher;

  @NonNull
  @ColumnInfo(name = "name")
  @SerializedName("name")
  public String Name;

  @ColumnInfo(name = "volume")
  @SerializedName("volume")
  public int Volume;

  @ColumnInfo(name = "added_date")
  public long AddedDate;

  @ColumnInfo(name = "updated_date")
  public long UpdatedDate;

  @Ignore
  @SerializedName("needs_review")
  public boolean NeedsReview;

  @Ignore
  public long SubmissionDate;

  @Ignore
  public String SubmittedBy;

  public SeriesEntity() {

    Id = BaseActivity.DEFAULT_PRODUCT_CODE;
    Publisher = "";
    Name = "";
    Volume = 0;
    AddedDate = Calendar.getInstance().getTimeInMillis();
    UpdatedDate = Calendar.getInstance().getTimeInMillis();

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
      "Series { Name=%s, Publisher=%s, Id=%s , Volume=%d }",
      Name,
      Publisher,
      Id,
      Volume);
  }

  /*
    Public Method(s)
   */
  @Exclude
  public boolean isValid() {

    return !Id.equals(BaseActivity.DEFAULT_PRODUCT_CODE) &&
      Id.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()&&
      !Name.isEmpty() &&
      !Publisher.isEmpty();
  }
}
