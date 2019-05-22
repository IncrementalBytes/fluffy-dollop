package net.frostedbytes.android.comiccollector.common;

import net.frostedbytes.android.comiccollector.models.ComicBook;

import java.util.Comparator;

public class SortUtils {

    public static class ByBookName implements Comparator<ComicBook> {

        public int compare(ComicBook a, ComicBook b) {

            return a.Title.compareTo(b.Title);
        }
    }

    public static class ByPublicationDate implements Comparator<ComicBook> {

        public int compare(ComicBook a, ComicBook b) {

            return Long.compare(b.PublishedDate, a.PublishedDate);
        }
    }

    public static class ByStringValue implements Comparator<String> {

        public int compare(String a, String b) {

            return a.compareTo(b);
        }
    }
}
