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
import com.google.gson.annotations.SerializedName;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(tableName = "publisher_table", indices = @Index(value = "id"))
public class PublisherEntity {

  @Ignore
  public static final String ROOT = "Publishers";

  @PrimaryKey()
  @NonNull
  @ColumnInfo(name = "id")
  @SerializedName("id")
  public String Id;

  @NonNull
  @ColumnInfo(name = "publisher_code")
  @SerializedName("publisher_code")
  public String PublisherCode;

  @NonNull
  @ColumnInfo(name = "name")
  @SerializedName("name")
  public String Name;

  @ColumnInfo(name = "active")
  @SerializedName("active")
  public boolean IsActive;

  @ColumnInfo(name = "added_date")
  public long AddedDate;

  @ColumnInfo(name = "updated_date")
  public long UpdatedDate;

  @Ignore
  @SerializedName("needs_review")
  public boolean NeedsReview;

  @Ignore
  @SerializedName("submitted_date")
  public long SubmissionDate;

  @Ignore
  @SerializedName("submitted_by")
  public String SubmittedBy;

  public PublisherEntity() {

    Id = UUID.randomUUID().toString();
    PublisherCode = BaseActivity.DEFAULT_PUBLISHER_CODE;
    Name = "";

    IsActive = true;
    NeedsReview = false;
    AddedDate = SubmissionDate = UpdatedDate = Calendar.getInstance().getTimeInMillis();
    SubmittedBy = "";
  }

  /*
  Object Override(s)
 */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "Publisher { Name=%s, Code=%s }",
      Name,
      PublisherCode);
  }

  public boolean isValid() {

    return !PublisherCode.equals(BaseActivity.DEFAULT_PUBLISHER_CODE) &&
      PublisherCode.length() == BaseActivity.DEFAULT_PUBLISHER_CODE.length() &&
      !Name.isEmpty();
  }

  public Map<String, Object> toMapping() {

    Map<String, Object> entity = new HashMap<>();
    entity.put("Id", Id);
    entity.put("PublisherCode", PublisherCode);
    entity.put("SubmittedBy", SubmittedBy);
    entity.put("Name", Name);
    entity.put("SubmissionDate", SubmissionDate);
    entity.put("NeedsReview", NeedsReview);
    return entity;
  }

  public static String getPublisherCode(String productCode) {

    if (productCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
      return productCode.substring(0, BaseActivity.DEFAULT_PUBLISHER_CODE.length());
    }

    return BaseActivity.DEFAULT_PUBLISHER_CODE;
  }
}
