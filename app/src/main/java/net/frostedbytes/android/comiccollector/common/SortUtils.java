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
package net.frostedbytes.android.comiccollector.common;

import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;
import net.frostedbytes.android.comiccollector.db.views.ComicSeriesDetails;

import java.util.Comparator;

public class SortUtils {

  public static class ByNumberOfIssues implements Comparator<ComicSeriesDetails> {

    public int compare(ComicSeriesDetails a, ComicSeriesDetails b) {

      return Long.compare(b.OwnedIssues.size(), a.OwnedIssues.size());
    }
  }

  public static class ByIssueNumber implements Comparator<ComicBookDetails> {

    public int compare(ComicBookDetails a, ComicBookDetails b) {

      return Long.compare(a.IssueNumber, b.IssueNumber);
    }
  }

  public static class ByYearAscending implements Comparator<Integer> {

    public int compare(Integer a, Integer b) {

      return Integer.compare(a, b);
    }
  }
}
