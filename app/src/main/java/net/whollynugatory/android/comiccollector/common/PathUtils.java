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
package net.whollynugatory.android.comiccollector.common;

import java.util.Locale;

public class PathUtils {

  public static String combine(Object... paths) {

    String finalPath = "";
    for (Object path : paths) {
      String format = "%s/%s";
      if (path.getClass() == Integer.class) {
        format = "%s/%d";
      }

      finalPath = String.format(Locale.US, format, finalPath, path);
    }

    return finalPath;
  }
}