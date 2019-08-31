package net.frostedbytes.android.comiccollector.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import net.frostedbytes.android.comiccollector.BaseActivity;

@Entity(tableName = "comic_publisher_table", indices = @Index(value = {"name"}))
public class ComicPublisher {

  @PrimaryKey()
  @NonNull
  @ColumnInfo(name = "id")
  @SerializedName("id")
  public String Id;

  @NonNull
  @ColumnInfo(name = "name")
  @SerializedName("name")
  public String Name;

  public ComicPublisher() {

    Id = BaseActivity.DEFAULT_COMIC_PUBLISHER_ID;
    Name = "";
  }

  public boolean isValid() {

    return !Id.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID) &&
      Id.length() == BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length() &&
      !Name.isEmpty();
  }
}
