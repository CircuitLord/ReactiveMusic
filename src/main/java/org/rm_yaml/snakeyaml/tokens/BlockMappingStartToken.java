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
package org.rm_yaml.snakeyaml.tokens;

import org.rm_yaml.snakeyaml.error.Mark;

/**
 * BlockMappingStartToken
 */
public final class BlockMappingStartToken extends Token {

  /**
   * Create
   *
   * @param startMark - start
   * @param endMark - end
   */
  public BlockMappingStartToken(Mark startMark, Mark endMark) {
    super(startMark, endMark);
  }

  /**
   * getter
   *
   * @return the identity
   */
  @Override
  public Token.ID getTokenId() {
    return ID.BlockMappingStart;
  }
}
