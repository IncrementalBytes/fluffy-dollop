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

public class SortUtils {

//  public static class ByNumberOfIssues implements Comparator<SeriesDetails> {
//
//    public int compare(SeriesDetails a, SeriesDetails b) {
//
//      return Long.compare(b.OwnedIssues.size(), a.OwnedIssues.size());
//    }
//  }

  public static class ByIssueNumber implements Comparator<ComicDetails> {

    public int compare(ComicDetails a, ComicDetails b) {

      return Long.compare(a.getIssueNumber(), b.getIssueNumber());
    }
  }

  public static class ByYearAscending implements Comparator<Integer> {

    public int compare(Integer a, Integer b) {

      return Integer.compare(a, b);
    }
  }
}
