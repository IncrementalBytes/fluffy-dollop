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
import java.io.Serializable;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.db.entity.ComicBookEntity;

@DatabaseView(
  "SELECT Book.id AS Id, " +
    "Book.product_code AS ProductCode, " +
    "Book.issue_code AS IssueCode," +
    "Book.title AS Title, " +
    "Book.published_date AS Published, " +
    "Book.owned AS IsOwned, " +
    "Book.read AS IsRead, " +
    "Series.name AS SeriesTitle, " +
    "Series.volume AS Volume, " +
    "Publishers.name AS PublisherName " +
  "FROM comic_book_table AS Book " +
  "INNER JOIN series_table AS Series ON Series.id = Book.product_code " +
  "INNER JOIN publisher_table AS Publishers ON Series.publisher_id = Publishers.id")
public class ComicBookDetails implements Serializable {

  public String Id;
  public String ProductCode;
  public String IssueCode;
  public String Title;
  public int IssueNumber;
  public String Published;
  public String SeriesTitle;
  public String PublisherName;
  public boolean IsOwned;
  public boolean IsRead;
  public int Volume;

  public ComicBookDetails() {

    Id = BaseActivity.DEFAULT_COMIC_BOOK_ID;
    IsOwned = false;
    IsRead = false;
    IssueCode = BaseActivity.DEFAULT_ISSUE_CODE;
    IssueNumber = 0;
    ProductCode = BaseActivity.DEFAULT_PRODUCT_CODE;
    Published = BaseActivity.DEFAULT_PUBLISHED_DATE;
    PublisherName = "";
    SeriesTitle = "";
    Title = "";
    Volume = 0;
  }

  public ComicBookDetails(ComicBookEntity comicBookEntity) {

    Id = comicBookEntity.Id;
    IsOwned = comicBookEntity.IsOwned;
    IsRead = comicBookEntity.HasRead;
    IssueCode = comicBookEntity.IssueCode;
    IssueNumber = 0; // TODO: update
    Published = comicBookEntity.PublishedDate;
    PublisherName = "";
    ProductCode = comicBookEntity.ProductCode;
    SeriesTitle = "";
    Title = comicBookEntity.Title;
    Volume = 0;
  }

  public ComicBookDetails(ComicBookDetails comicBookDetails) {

    Id = comicBookDetails.Id;
    IsOwned = comicBookDetails.IsOwned;
    IsRead = comicBookDetails.IsRead;
    IssueCode = comicBookDetails.IssueCode;
    IssueNumber = comicBookDetails.IssueNumber;
    ProductCode = comicBookDetails.ProductCode;
    Published = comicBookDetails.Published;
    PublisherName = comicBookDetails.PublisherName;
    SeriesTitle = comicBookDetails.SeriesTitle;
    Title = comicBookDetails.Title;
    Volume = comicBookDetails.Volume;
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "ComicBook { Title=%s, ProductCode=%s, Issue=%d, %s, %s}",
      Title,
      ProductCode,
      IssueNumber,
      IsOwned ? "Owned" : "OnWishlist",
      IsRead ? "Read" : "Unread");
  }
}
