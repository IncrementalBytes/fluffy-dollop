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

package net.whollynugatory.android.comiccollector.common;

import net.whollynugatory.android.comiccollector.db.views.ComicDetails;

import java.util.Comparator;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;

public class SortUtils {

  public static class ByNumberOfIssues implements Comparator<SeriesDetails> {

    public int compare(SeriesDetails a, SeriesDetails b) {

      return Long.compare(b.BookCount, a.BookCount);
    }
  }

  public static class BySeriesTitle implements Comparator<SeriesDetails> {

    public int compare(SeriesDetails a, SeriesDetails b) {

      return a.SeriesTitle.compareTo(b.SeriesTitle);
    }
  }

  public static class ByVolume implements Comparator<SeriesDetails> {

    public int compare(SeriesDetails a, SeriesDetails b) {

      return Long.compare(b.Volume, a.Volume);
    }
  }

  public static class ByIssueNumber implements Comparator<ComicDetails> {

    public int compare(ComicDetails a, ComicDetails b) {

      return Long.compare(a.getIssueNumber(), b.getIssueNumber());
    }
  }

  public static class ByAddedDate implements Comparator<ComicDetails> {

    public int compare(ComicDetails a, ComicDetails b) {

      return Long.compare(a.AddedDate, b.AddedDate);
    }
  }

  public static class ByTitle implements Comparator<ComicDetails> {

    public int compare(ComicDetails a, ComicDetails b) {

      return a.Title.compareTo(b.Title);
    }
  }

  public static class ByUpdatedDate implements Comparator<ComicDetails> {

    public int compare(ComicDetails a, ComicDetails b) {

      return Long.compare(a.UpdatedDate, b.UpdatedDate);
    }
  }
}
