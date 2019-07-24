package net.frostedbytes.android.comiccollector.common;

import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.util.Comparator;
import net.frostedbytes.android.comiccollector.models.ComicSeries;

public class SortUtils {

  public static class ByNumberOfIssues implements Comparator<ComicSeries> {

    public int compare(ComicSeries a, ComicSeries b) {

      return Long.compare(b.ComicBooks.size(), a.ComicBooks.size());
    }
  }

  public static class ByPublicationDateAndIssueNumber implements Comparator<ComicBook> {

    public int compare(ComicBook a, ComicBook b) {

      if (a.PublishedDate.equals(b.PublishedDate)) {
        return Long.compare(b.IssueNumber, a.IssueNumber);
      } else {
        return b.PublishedDate.compareTo(a.PublishedDate);
      }
    }
  }
}
