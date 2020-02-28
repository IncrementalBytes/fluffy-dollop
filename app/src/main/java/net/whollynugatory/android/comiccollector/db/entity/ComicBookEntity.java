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
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(
  tableName = "comic_book_table",
  foreignKeys = {
    @ForeignKey(entity = PublisherEntity.class, parentColumns = "id", childColumns = "publisher_id"),
    @ForeignKey(entity = SeriesEntity.class, parentColumns = "id", childColumns = "series_id")
  },
  indices = {
    @Index(value = "id"),
    @Index(value = "publisher_id"),
    @Index(value = "series_id")
  })
public class ComicBookEntity implements Serializable {

  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  public String Id;

  @NonNull
  @ColumnInfo(name = "publisher_id")
  @SerializedName("publisher_id")
  public String PublisherId;

  @NonNull
  @ColumnInfo(name = "series_id")
  @SerializedName("series_id")
  public String SeriesId;

  @NonNull
  @ColumnInfo(name = "issue_code")
  @SerializedName("issue_code")
  public String IssueCode;

  @NonNull
  @ColumnInfo(name = "title")
  @SerializedName("title")
  public String Title;

  @ColumnInfo(name = "active")
  @SerializedName("active")
  public boolean IsActive;

  @ColumnInfo(name = "owned")
  @SerializedName("owned")
  public boolean IsOwned;

  @ColumnInfo(name = "read")
  @SerializedName("read")
  public boolean HasRead;

  @NonNull
  @ColumnInfo(name = "published_date")
  @SerializedName("publish_date")
  public String PublishedDate;

  @ColumnInfo(name = "added_date")
  public transient long AddedDate;


  @ColumnInfo(name = "updated_date")
  public transient long UpdatedDate;

  public ComicBookEntity() {

    Id = UUID.randomUUID().toString();
    PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
    SeriesId = BaseActivity.DEFAULT_SERIES_ID;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    HasRead = false;
    IsActive = true;
    IsOwned = false;
    PublishedDate = "";
    Title = "";

    AddedDate = Calendar.getInstance().getTimeInMillis();
    UpdatedDate = Calendar.getInstance().getTimeInMillis();
  }

  public boolean isValid() {
    return !Id.equals(BaseActivity.DEFAULT_ID) &&
      Id.length() == BaseActivity.DEFAULT_ID.length() &&
      !PublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) &&
      PublisherId.length() == BaseActivity.DEFAULT_PUBLISHER_ID.length() &&
      !SeriesId.equals(BaseActivity.DEFAULT_SERIES_ID) &&
      SeriesId.length() == BaseActivity.DEFAULT_SERIES_ID.length() &&
      !IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) &&
      IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length();
  }
}
