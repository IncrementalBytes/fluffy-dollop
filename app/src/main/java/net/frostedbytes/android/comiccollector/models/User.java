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
package net.frostedbytes.android.comiccollector.models;

import java.io.Serializable;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class User implements Serializable {

  private static final String TAG = BaseActivity.BASE_TAG + "User";

  public static final String ROOT = "Users";

  public String Email;

  public String FullName;

  public String Id;

  public boolean IsGeek;

  public boolean ShowBarcodeHint;

  public boolean UseImageCapture;

  public User() {

    this.Email = "";
    this.FullName = "";
    this.Id = BaseActivity.DEFAULT_USER_ID;
    this.IsGeek = false;
    this.ShowBarcodeHint = true;
    this.UseImageCapture = false;
  }

  public static boolean isValid(User user) {

    LogUtils.debug(TAG, "++isValid(User)");
    return user != null &&
      !user.Id.isEmpty() &&
      !user.Id.equals(BaseActivity.DEFAULT_USER_ID) &&
      user.Id.length() == BaseActivity.DEFAULT_USER_ID.length();
  }
}
