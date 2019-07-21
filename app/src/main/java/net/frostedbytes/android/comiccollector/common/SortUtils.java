package net.frostedbytes.android.comiccollector.common;

import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.util.Comparator;

public class SortUtils {

  public static class ByBookName implements Comparator<ComicBook> {

    public int compare(ComicBook a, ComicBook b) {

      return a.Title.compareTo(b.Title);
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
