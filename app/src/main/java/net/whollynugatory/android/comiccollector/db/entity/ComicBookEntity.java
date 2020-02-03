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
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Calendar;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(
  tableName = "comic_book_table",
  foreignKeys = @ForeignKey(entity = SeriesEntity.class, parentColumns = "id", childColumns = "product_code"),
  indices = @Index(value = {"product_code", "issue_code"}, unique = true))
public class ComicBookEntity implements Serializable {

  @Ignore
  private int mCoverVariant;

  @Ignore
  private int mIssueNumber;

  @Ignore
  private int mPrintRun;

  @Ignore
  private String mPublisherId;

  @Ignore
  private String mSeriesId;

  /**
   * Unique identifier for comic. A combination of the product code and issue code.
   **/
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

    mCoverVariant = 0;
    mIssueNumber = 0;
    mPrintRun = 0;
    mPublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
    mSeriesId = BaseActivity.DEFAULT_SERIES_ID;

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

  public ComicBookEntity(String productCode, String issueCode) {

    mCoverVariant = 0;
    mIssueNumber = 0;
    mPrintRun = 0;
    mPublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
    mSeriesId = BaseActivity.DEFAULT_SERIES_ID;

    ProductCode = productCode;
    IssueCode = issueCode;
    Id = String.format("%s-%s", productCode, issueCode);

    HasRead = false;
    IsOwned = false;
    PublishedDate = "";
    Title = "";

    AddedDate = Calendar.getInstance().getTimeInMillis();
    UpdatedDate = Calendar.getInstance().getTimeInMillis();
  }

  public ComicBookEntity(ComicBookEntity comicBookEntity) {

    mCoverVariant = comicBookEntity.getCoverVariant();
    mIssueNumber = comicBookEntity.getIssueNumber();
    mPrintRun = comicBookEntity.getPrintRun();
    mPublisherId = comicBookEntity.getPublisherId();
    mSeriesId = comicBookEntity.getSeriesId();

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

  @Ignore
  public int getCoverVariant() {

    if (mCoverVariant == 0 &&
      (!IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) && IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length())) {
      try {
        String temp = IssueCode.substring(IssueCode.length() - 2, IssueCode.length() - 1);
        mCoverVariant = Integer.parseInt(temp);
      } catch (Exception e) {
        mCoverVariant = -1;
      }
    }

    return mCoverVariant;
  }

  @Ignore
  public int getIssueNumber() {

    if (mIssueNumber == 0 &&
      (!IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) && IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length())) {
      try {
        String temp = IssueCode.substring(0, IssueCode.length() - 2);
        mIssueNumber = Integer.parseInt(temp);
      } catch (Exception e) {
        mIssueNumber = -1;
      }
    }

    return mIssueNumber;
  }

  @Ignore
  public int getPrintRun() {

    if (mPrintRun == 0 &&
      (!IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) && IssueCode.length() == BaseActivity.DEFAULT_ISSUE_CODE.length())) {
      try {
        String temp = IssueCode.substring(IssueCode.length() - 1);
        mPrintRun = Integer.parseInt(temp);
      } catch (Exception e) {
        mPrintRun = -1;
      }
    }

    return mPrintRun;
  }

  @Ignore
  public String getPublisherId() {

    if ((!ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE) && ProductCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) &&
      mPublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) || mPublisherId.length() != BaseActivity.DEFAULT_PUBLISHER_ID.length()) {
      try {
        mPublisherId = ProductCode.substring(0, BaseActivity.DEFAULT_PUBLISHER_ID.length());
      } catch (Exception e) {
        mPublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
      }
    }

    return mPublisherId;
  }

  @Ignore
  public String getSeriesId() {

    if ((!ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE) && ProductCode.length() == BaseActivity.DEFAULT_PRODUCT_CODE.length()) &&
      mSeriesId.equals(BaseActivity.DEFAULT_SERIES_ID) || mSeriesId.length() != BaseActivity.DEFAULT_SERIES_ID.length()) {
      try {
        mSeriesId = ProductCode.substring(BaseActivity.DEFAULT_SERIES_ID.length());
      } catch (Exception e) {
        mSeriesId = BaseActivity.DEFAULT_SERIES_ID;
      }
    }

    return mSeriesId;
  }
}
