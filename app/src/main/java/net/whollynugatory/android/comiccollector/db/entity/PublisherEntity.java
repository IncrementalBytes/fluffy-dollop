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
package net.whollynugatory.android.comiccollector.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

@Entity(tableName = "publisher_table", indices = @Index(value = {"name"}))
public class PublisherEntity {

  @PrimaryKey()
  @NonNull
  @ColumnInfo(name = "id")
  @SerializedName("id")
  public String Id;

  @NonNull
  @ColumnInfo(name = "name")
  @SerializedName("name")
  public String Name;

  public PublisherEntity() {

    Id = BaseActivity.DEFAULT_PUBLISHER_ID;
    Name = "";
  }

  public boolean isValid() {

    return !Id.equals(BaseActivity.DEFAULT_PUBLISHER_ID) &&
      Id.length() == BaseActivity.DEFAULT_PUBLISHER_ID.length() &&
      !Name.isEmpty();
  }
}
