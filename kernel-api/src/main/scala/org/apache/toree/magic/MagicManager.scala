/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.magic

import org.slf4j.LoggerFactory
import scala.language.dynamics

class MagicManager extends Dynamic {
  protected val logger = LoggerFactory.getLogger(this.getClass.getName)
  /**
   * Checks if the provided magic is a line magic.
   *
   * @param magic The magic instance
   * @return True if the magic is an instance of a line magic
   */
  def isLineMagic(magic: Magic): Boolean =
    magic.getClass.getInterfaces.contains(classOf[LineMagic])

  /**
   * Checks if the provided magic is a cell magic.
   *
   * @param magic The magic instance
   * @return True if the magic is an instance of a cell magic
   */
  def isCellMagic(magic: Magic): Boolean =
    magic.getClass.getInterfaces.contains(classOf[CellMagic])

  /**
   * Finds a magic whose class name ends with a case insensitive name.
   *
   * @param name The name to search for
   * @return The magic
   * @throws MagicNotFoundException when no magics match name
   */
  @throws[MagicNotFoundException]
  def findMagic(name: String): Magic = {

    // TODO: Find a magic by passing a name.
    null
  }
}
