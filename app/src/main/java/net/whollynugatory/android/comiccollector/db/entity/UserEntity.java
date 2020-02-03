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

import android.util.Log;
import java.io.Serializable;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class UserEntity implements Serializable {

  private static final String TAG = BaseActivity.BASE_TAG + "UserEntity";

  public static final String ROOT = "Users";

  public String Email;

  public String FullName;

  public String Id;

  public boolean IsGeek;

  public boolean ShowBarcodeHint;

  public boolean UseImageCapture;

  public UserEntity() {

    this.Email = "";
    this.FullName = "";
    this.Id = BaseActivity.DEFAULT_USER_ID;
    this.IsGeek = false;
    this.ShowBarcodeHint = true;
    this.UseImageCapture = false;
  }

  public static boolean isValid(UserEntity userEntity) {

    Log.d(TAG, "++isValid(UserEntity)");
    return userEntity != null &&
      !userEntity.Id.isEmpty() &&
      !userEntity.Id.equals(BaseActivity.DEFAULT_USER_ID) &&
      userEntity.Id.length() == BaseActivity.DEFAULT_USER_ID.length();
  }
}
