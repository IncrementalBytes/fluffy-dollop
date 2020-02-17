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
import java.util.UUID;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;

@DatabaseView(
  "SELECT Book.id AS Id, " +
    "Book.product_code AS ProductCode, " +
    "Book.issue_code AS IssueCode," +
    "Book.title AS Title, " +
    "Book.published_date AS Published, " +
    "Book.issue_number AS IssueNumber, " +
    "Book.cover_variant AS CoverVariant, " +
    "Book.print_run AS PrintRun, " +
    "Book.owned AS IsOwned, " +
    "Book.read AS HasRead, " +
    "Series.name AS SeriesTitle, " +
    "Series.volume AS Volume, " +
    "Series.Publisher AS PublisherName, " +
    "Book.added_date AS AddedDate, " +
    "Book.updated_date AS UpdatedDate " +
  "FROM comic_book_table AS Book " +
  "INNER JOIN series_table AS Series ON Series.id = Book.product_code")
public class ComicDetails implements Serializable {

  public String Id;
  public String ProductCode;
  public String IssueCode;
  public String Title;
  public String Published;
  public int IssueNumber;
  public int CoverVariant;
  public int PrintRun;
  public boolean IsOwned;
  public boolean HasRead;
  public String SeriesTitle;
  public int Volume;
  public String PublisherName;
  public long AddedDate;
  public long UpdatedDate;

  public ComicDetails() {

    Id = UUID.randomUUID().toString();
    ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    Title = "";
    Published = BaseActivity.DEFAULT_PUBLISHED_DATE;
    IssueNumber = 0;
    CoverVariant = 0;
    PrintRun = 0;
    IsOwned = false;
    HasRead = false;
    SeriesTitle = "";
    Volume = 0;
    PublisherName = "";
    AddedDate = 0;
    UpdatedDate = 0;
  }

  public ComicDetails(ComicBookEntity comicBookEntity) {

    Id = comicBookEntity.Id;
    ProductCode = comicBookEntity.ProductCode;
    IssueCode = comicBookEntity.IssueCode;
    Title = comicBookEntity.Title;
    Published = comicBookEntity.PublishedDate;
    IssueNumber = comicBookEntity.IssueNumber;
    CoverVariant = comicBookEntity.CoverVariant;
    PrintRun = comicBookEntity.PrintRun;
    IsOwned = comicBookEntity.IsOwned;
    HasRead = comicBookEntity.HasRead;
    SeriesTitle = "";
    Volume = 0;
    PublisherName = "";
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
      ProductCode,
      IssueCode,
      IsOwned ? "Owned" : "Not Owned",
      HasRead ? "Read" : "Unread");
  }

  @Ignore
  public void setIssueCode(String issueCode) {

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
  }

  @Ignore
  public ComicBookEntity toEntity() {

    ComicBookEntity entity = new ComicBookEntity();

    entity.Id = Id;
    entity.ProductCode = ProductCode;
    entity.IssueCode = IssueCode;

    entity.CoverVariant = CoverVariant;
    entity.HasRead = HasRead;
    entity.IssueNumber = IssueNumber;
    entity.IsOwned = IsOwned;
    entity.PrintRun = PrintRun;
    entity.PublishedDate = Published;
    entity.Title = Title;

    entity.AddedDate = Calendar.getInstance().getTimeInMillis();
    entity.UpdatedDate = Calendar.getInstance().getTimeInMillis();
    return entity;
  }
}
