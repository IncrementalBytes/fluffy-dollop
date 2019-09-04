package net.frostedbytes.android.comiccollector.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import net.frostedbytes.android.comiccollector.db.entity.ComicBook;
import net.frostedbytes.android.comiccollector.db.views.ComicBookDetails;

@Dao
public interface ComicBookDao {

  @Query("DELETE from comic_book_table")
  void deleteAll();

  @Query("DELETE from comic_book_table WHERE Id == :comicBookId")
  void deleteById(String comicBookId);

  @Query("SELECT * from comic_book_table")
  LiveData<List<ComicBook>> exportable();

  @Query("SELECT * from ComicBookDetails WHERE Id == :comicBookId")
  LiveData<ComicBookDetails> get(String comicBookId);

  @Query("SELECT * from ComicBookDetails WHERE ProductCode == :productCode AND IssueCode == :issueCode")
  LiveData<ComicBookDetails> get(String productCode, String issueCode);

  @Query("SELECT * from ComicBookDetails ORDER BY published DESC")
  LiveData<List<ComicBookDetails>> getAll();

  @Query("SELECT * from ComicBookDetails WHERE ProductCode == :productCode ORDER BY IssueNumber DESC")
  LiveData<List<ComicBookDetails>> getByProductCode(String productCode);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(ComicBook book);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<ComicBook> comicBooks);
}
