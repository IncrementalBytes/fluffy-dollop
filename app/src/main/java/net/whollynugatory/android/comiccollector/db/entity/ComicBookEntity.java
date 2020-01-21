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
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(
  tableName = "comic_book_table",
  foreignKeys = @ForeignKey(entity = SeriesEntity.class, parentColumns = "id", childColumns = "product_code"),
  indices = @Index(value = {"product_code", "issue_code"}, unique = true))
public class ComicBookEntity implements Serializable {

  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  public String Id;

  @ColumnInfo(name = "product_code")
  @SerializedName("product_code")
  public String ProductCode;

  @NonNull
  @ColumnInfo(name = "issue_code")
  @SerializedName("issue_code")
  public String IssueCode;

  @NonNull
  @ColumnInfo(name = "title")
  @SerializedName("title")
  public String Title;

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
  public long AddedDate;

  @ColumnInfo(name = "updated_date")
  public long UpdatedDate;

  public ComicBookEntity() {

    Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
    ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
    IsOwned = false;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    PublishedDate = "";
    HasRead = false;
    Title = "";

    AddedDate = Calendar.getInstance().getTimeInMillis();
    UpdatedDate = Calendar.getInstance().getTimeInMillis();
  }

  public ComicBookEntity(
    @NonNull String id,
    @NonNull String productCode,
    @NonNull String issueCode,
    boolean owned,
    boolean read,
    @NonNull String title,
    @NonNull String publishedDate) {

    Id = id;
    ProductCode = productCode;
    IssueCode = issueCode;
    IsOwned = owned;
    HasRead = read;
    Title = title;
    PublishedDate = publishedDate;
  }

  public ComicBookEntity(ComicBookEntity comicBookEntity) {

    Id = comicBookEntity.Id;
    ProductCode = comicBookEntity.ProductCode;
    IssueCode = comicBookEntity.IssueCode;
    IsOwned = comicBookEntity.IsOwned;
    HasRead = comicBookEntity.HasRead;
    Title = comicBookEntity.Title;
    PublishedDate = comicBookEntity.PublishedDate;

    AddedDate = comicBookEntity.AddedDate;
    UpdatedDate = comicBookEntity.UpdatedDate;
  }

 /*
    Public Method(s)
   */
  /**
   * Attempts to extract the Publisher, Series, and IssueCode identifiers from a product code.
   * @param productCode 12-5 character string representing the full product code.
   */
  public void parseProductCode(String productCode) {

    String[] segments = productCode.split("-");
    ProductCode = segments[0];
    if (ProductCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length() && !ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE)) {
      try {
        Id = String.format(Locale.US, "%s-%s", ProductCode, BaseActivity.DEFAULT_ISSUE_CODE);
//        PublisherId = ProductCode.substring(0, BaseActivity.DEFAULT_PUBLISHER_ID.length());
//        SeriesId = ProductCode.substring(BaseActivity.DEFAULT_PUBLISHER_ID.length());
      } catch (Exception e) {
        Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
        ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
//        PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
//        SeriesId = BaseActivity.DEFAULT_SERIES_ID;
      }
    }

    if (segments.length > 1) { // grab issue number
      Id = productCode;
      IssueCode = segments[1];
    }
  }
}
