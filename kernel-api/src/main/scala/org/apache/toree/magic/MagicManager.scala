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

import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.magic.dependencies.IncludeKernel
import org.slf4j.LoggerFactory
import scala.language.dynamics

import org.apache.toree.utils.ClassUtil

class MagicManager(kernel: KernelLike) extends Dynamic {
  private val magics: Map[String, Magic] = Map(
    "lsmagic" -> loadMagic("org.apache.toree.magic.builtin.LSMagic"),
    "sql" -> loadMagic("org.apache.toree.magic.builtin.Sql"),
    "sparksql" -> loadMagic("org.apache.toree.magic.builtin.Sql"),
    "scala" -> loadMagic("org.apache.toree.magic.builtin.Scala"),
    "sparkr" -> loadMagic("org.apache.toree.magic.builtin.SparkR"),
    "html" -> loadMagic("org.apache.toree.magic.builtin.Html"),
    "javascript" -> loadMagic("org.apache.toree.magic.builtin.JavaScript"),
    "pyspark" -> loadMagic("org.apache.toree.magic.builtin.PySpark"),
    "dataframe" -> loadMagic("org.apache.toree.magic.builtin.DataFrame"),
    "addjar" -> loadMagic("org.apache.toree.magic.builtin.AddJar"),
    "adddeps" -> loadMagic("org.apache.toree.magic.builtin.AddDeps")
  )

  private def loadMagic(className: String): Magic = {
    val magic = ClassUtil.forName(className).newInstance().asInstanceOf[Magic]
    magic match {
      case includeKernel: IncludeKernel => includeKernel.init(kernel)
      case _ => Unit
    }
    magic
  }

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
    magics.getOrElse(name.toLowerCase, null)
  }
}
