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
import java.util.Calendar;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;

@DatabaseView(
  "SELECT Book.id AS Id, " +
    "Publishers.id AS PublisherId, " +
    " Publishers.publisher_code AS PublisherCode, " +
    "Series.id AS SeriesId, " +
    "Series.series_code AS SeriesCode, " +
    "Book.issue_code AS IssueCode," +
    "Book.title AS Title, " +
    "Series.name AS SeriesTitle, " +
    "Series.volume AS Volume, " +
    "Publishers.Name AS Publisher, " +
    "Book.published_date AS Published, " +
    "Book.read AS HasRead, " +
    "Book.owned AS IsOwned, " +
    "Book.added_date AS AddedDate, " +
    "Book.updated_date AS UpdatedDate " +
  "FROM comic_book_table AS Book " +
  "INNER JOIN series_table AS Series ON Series.id = Book.series_id " +
  "INNER JOIN publisher_table AS Publishers ON Publishers.id = Series.publisher_id")
public class ComicDetails implements Serializable {

  @Ignore
  private int mCoverVariant;

  @Ignore
  private int mIssueNumber;

  @Ignore
  private int mPrintRun;

  public String Id;

  public String IssueCode;
  public String Title;
  public String Published;
  public boolean HasRead;
  public boolean IsOwned;

  public String SeriesCode;
  public String SeriesId;
  public String SeriesTitle;
  public int Volume;

  public String PublisherCode;
  public String PublisherId;
  public String Publisher;

  public long AddedDate;
  public long UpdatedDate;

  public ComicDetails() {

    Id = BaseActivity.DEFAULT_ID;
    setIssueCode(BaseActivity.DEFAULT_ISSUE_CODE);
    Title = "";
    Published = BaseActivity.DEFAULT_PUBLISHED_DATE;
    HasRead = false;
    IsOwned = false;

    SeriesCode = BaseActivity.DEFAULT_SERIES_CODE;
    SeriesId = BaseActivity.DEFAULT_SERIES_ID;
    SeriesTitle = "";
    Volume = 0;

    PublisherCode = BaseActivity.DEFAULT_PUBLISHER_CODE;
    PublisherId = BaseActivity.DEFAULT_PUBLISHER_ID;
    Publisher = "";

    AddedDate = 0;
    UpdatedDate = 0;
  }

  public ComicDetails(ComicBookEntity comicBookEntity) {

    Id = comicBookEntity.Id;
    setIssueCode(comicBookEntity.IssueCode);
    Title = comicBookEntity.Title;
    Published = comicBookEntity.PublishedDate;
    HasRead = comicBookEntity.HasRead;
    IsOwned = comicBookEntity.IsOwned;

    SeriesId = comicBookEntity.SeriesId;
    SeriesTitle = "";
    Volume = 0;

    PublisherId = comicBookEntity.PublisherId;
    Publisher = "";

    AddedDate = comicBookEntity.AddedDate;
    UpdatedDate = comicBookEntity.UpdatedDate;
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "ComicDetails { Title=%s, ProductCode=%s, IssueCode=%s, %s, %s}",
      Title,
      getProductCode(),
      IssueCode,
      HasRead ? "Read" : "Unread",
      IsOwned ? "Owned" : "Not Owned");
  }

  @Ignore
  public int getCoverVariant() {

    if (mCoverVariant < 0) {
      try {
        String temp = IssueCode.substring(IssueCode.length() - 2, IssueCode.length() - 1);
        mCoverVariant = Integer.parseInt(temp);
      } catch (Exception e) {
        return -1;
      }
    }

    return mCoverVariant;
  }

  @Ignore
  public int getIssueNumber() {

    if (mIssueNumber < 0) {
      try {
        String temp = IssueCode.substring(0, IssueCode.length() - 2);
        mIssueNumber = Integer.parseInt(temp);
      } catch (Exception e) {
        return -1;
      }
    }

    return mIssueNumber;
  }

  @Ignore
  public int getPrintRun() {

    if (mPrintRun < 0) {
      try {
        String temp = IssueCode.substring(IssueCode.length() - 1);
        mPrintRun = Integer.parseInt(temp);
      } catch (Exception e) {
        return -1;
      }
    }

    return mPrintRun;
  }

  @Ignore
  public String getProductCode() {

    return String.format(Locale.US, "%s%s", PublisherCode, SeriesCode);
  }

  @Ignore
  public void setIssueCode(String issueCode) {

    IssueCode = issueCode;
    mCoverVariant = -1;
    mIssueNumber = -1;
    mPrintRun = -1;
  }

  @Ignore
  public ComicBookEntity toEntity() {

    ComicBookEntity entity = new ComicBookEntity();

    entity.Id = Id;
    entity.IssueCode = IssueCode;
    entity.Title = Title;
    entity.PublishedDate = Published;
    entity.HasRead = HasRead;
    entity.IsOwned = IsOwned;

    entity.SeriesId = SeriesId;

    entity.PublisherId = PublisherId;

    entity.AddedDate = Calendar.getInstance().getTimeInMillis();
    entity.UpdatedDate = Calendar.getInstance().getTimeInMillis();
    return entity;
  }

  @Ignore
  public SeriesDetails toSeriesDetails() {

    SeriesDetails seriesDetails = new SeriesDetails();
    seriesDetails.PublisherId = PublisherId;
    seriesDetails.PublisherCode = PublisherCode;
    seriesDetails.Publisher = Publisher;
    seriesDetails.SeriesId = SeriesId;
    seriesDetails.SeriesCode = SeriesCode;
    seriesDetails.SeriesTitle = SeriesTitle;
    seriesDetails.Volume = Volume;
    return seriesDetails;
  }
}
