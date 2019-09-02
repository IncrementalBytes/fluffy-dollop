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
