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
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(tableName = "comic_book_table")
public class ComicBookEntity implements Serializable {

  @Ignore
  private String mPublisherId;

  @Ignore
  private String mSeriesId;

  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  public String Id;

  @NonNull
  @ColumnInfo(name = "product_code")
  @SerializedName("product_code")
  public String ProductCode;

  @NonNull
  @ColumnInfo(name = "issue_code")
  @SerializedName("issue_code")
  public String IssueCode;

  @ColumnInfo(name = "cover_variant")
  @SerializedName("cover_variant")
  public int CoverVariant;

  @ColumnInfo(name = "issue_number")
  @SerializedName("issue_number")
  public int IssueNumber;

  @ColumnInfo(name = "print_run")
  @SerializedName("print_run")
  public int PrintRun;

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

    mPublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
    mSeriesId = BaseActivity.DEFAULT_SERIES_ID;

    Id = UUID.randomUUID().toString();
    ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    CoverVariant = 0;
    HasRead = false;
    IsOwned = false;
    IssueNumber = 0;
    PrintRun = 0;
    PublishedDate = "";
    Title = "";

    AddedDate = Calendar.getInstance().getTimeInMillis();
    UpdatedDate = Calendar.getInstance().getTimeInMillis();
  }

  public ComicBookEntity(String productCode, @NonNull String issueCode) {

    mPublisherId = productCode.substring(0, BaseActivity.DEFAULT_PUBLISHER_ID.length());
    mSeriesId = productCode.substring(BaseActivity.DEFAULT_SERIES_ID.length());

    Id = UUID.randomUUID().toString();
    ProductCode = productCode;
    IssueCode = issueCode;

    try {
      String temp = IssueCode.substring(IssueCode.length() - 2, IssueCode.length() - 1);
      CoverVariant = Integer.parseInt(temp);
    } catch (Exception e) {
      CoverVariant = -1;
    }

    HasRead = false;
    IsOwned = false;
    try {
      String temp = IssueCode.substring(0, IssueCode.length() - 2);
      IssueNumber = Integer.parseInt(temp);
    } catch (Exception e) {
      IssueNumber = -1;
    }

    try {
      String temp = IssueCode.substring(IssueCode.length() - 1);
      PrintRun = Integer.parseInt(temp);
    } catch (Exception e) {
      PrintRun = -1;
    }

    PublishedDate = "";
    Title = "";

    AddedDate = Calendar.getInstance().getTimeInMillis();
    UpdatedDate = Calendar.getInstance().getTimeInMillis();
  }

  public ComicBookEntity(ComicBookEntity comicBookEntity) {

    mPublisherId = comicBookEntity.getPublisherId();
    mSeriesId = comicBookEntity.getSeriesId();

    Id = comicBookEntity.Id;
    ProductCode = comicBookEntity.ProductCode;
    IssueCode = comicBookEntity.IssueCode;

    CoverVariant = comicBookEntity.CoverVariant;
    HasRead = comicBookEntity.HasRead;
    IssueNumber = comicBookEntity.IssueNumber;
    IsOwned = comicBookEntity.IsOwned;
    PrintRun = comicBookEntity.PrintRun;
    PublishedDate = comicBookEntity.PublishedDate;
    Title = comicBookEntity.Title;

    AddedDate = comicBookEntity.AddedDate;
    UpdatedDate = comicBookEntity.UpdatedDate;
  }

  @Ignore
  public String getPublisherId() {

    if (mPublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) || mPublisherId.length() != BaseActivity.DEFAULT_PUBLISHER_ID.length()) {
      if (!ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE) && ProductCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
        try {
          mPublisherId = ProductCode.substring(0, BaseActivity.DEFAULT_PUBLISHER_ID.length());
        } catch (Exception e) {
          mPublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
        }
      } else {
        mPublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
      }
    }

    return mPublisherId;
  }

  @Ignore
  public String getSeriesId() {

    if (mSeriesId.equals(BaseActivity.DEFAULT_SERIES_ID) || mSeriesId.length() != BaseActivity.DEFAULT_SERIES_ID.length()) {
      if (!ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE) && ProductCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) {
        try {
          mSeriesId = ProductCode.substring(BaseActivity.DEFAULT_SERIES_ID.length());
        } catch (Exception e) {
          mSeriesId = BaseActivity.DEFAULT_SERIES_ID;
        }
      } else {
        mSeriesId = BaseActivity.DEFAULT_SERIES_ID;
      }
    } else {
      mSeriesId = BaseActivity.DEFAULT_SERIES_ID;
    }

    return mSeriesId;
  }
}
