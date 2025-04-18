/**
 * Copyright (c) 2008, SnakeYAML
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.rm_yaml.snakeyaml.error;

public class YAMLException extends RuntimeException {

  private static final long serialVersionUID = -4738336175050337570L;

  public YAMLException(String message) {
    super(message);
  }

  public YAMLException(Throwable cause) {
    super(cause);
  }

  public YAMLException(String message, Throwable cause) {
    super(message, cause);
  }
}
