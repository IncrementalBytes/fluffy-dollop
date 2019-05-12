package net.frostedbytes.android.comiccollector.common;

import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.util.Comparator;

public class SortUtils {

    public static class ByDateAdded implements Comparator<ComicBook> {

        public int compare(ComicBook a, ComicBook b) {

            return Long.compare(a.AddedDate, b.AddedDate);
        }
    }

    public static class ByBookName implements Comparator<ComicBook> {

        public int compare(ComicBook a, ComicBook b) {

            return a.Title.compareTo(b.Title);
        }
    }
}
