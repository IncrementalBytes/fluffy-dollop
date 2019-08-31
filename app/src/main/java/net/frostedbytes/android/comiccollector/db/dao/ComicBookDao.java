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

  @Query("DELETE FROM comic_book_table WHERE id == :comicBookId")
  void delete(String comicBookId);

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
}
