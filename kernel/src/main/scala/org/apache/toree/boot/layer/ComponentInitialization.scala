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

package org.apache.toree.boot.layer

import java.io.File
import java.net.URL
import java.nio.file.{Files, Paths}
import java.util.concurrent.ConcurrentHashMap
import akka.actor.ActorRef
import com.typesafe.config.Config
import org.apache.spark.SparkConf
import org.apache.toree.dependencies.{CoursierDependencyDownloader, Credentials, DependencyDownloader}
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.Kernel
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.magic.MagicManager
import org.apache.toree.utils.{LogLike, FileUtils}
import scala.collection.JavaConverters._

/**
 * Represents the component initialization. All component-related pieces of the
 * kernel (non-actors) should be created here. Limited items should be exposed.
 */
trait ComponentInitialization {
  /**
   * Initializes and registers all components (not needed by bare init).
   *
   * @param config The config used for initialization
   * @param actorLoader The actor loader to use for some initialization
   */
  def initializeComponents(
    config: Config, actorLoader: ActorLoader
  ): (Interpreter,
    Kernel, DependencyDownloader, MagicManager,
    collection.mutable.Map[String, ActorRef])
}

/**
 * Represents the standard implementation of ComponentInitialization.
 */
trait StandardComponentInitialization extends ComponentInitialization {
  this: LogLike =>

  /**
   * Initializes and registers all components (not needed by bare init).
   *
   * @param config The config used for initialization
   * @param actorLoader The actor loader to use for some initialization
   */
  def initializeComponents(
    config: Config, actorLoader: ActorLoader
  ) = {

    val interpreterManager =  InterpreterManager(config)
    interpreterManager.interpreters foreach(println)

    val dependencyDownloader = initializeDependencyDownloader(config)

    val kernel = initializeKernel(config, actorLoader, interpreterManager, dependencyDownloader)

    interpreterManager.initializeInterpreters(kernel)

    val responseMap = initializeResponseMap()

    (interpreterManager.defaultInterpreter.get, kernel,
      dependencyDownloader, kernel.magics, responseMap)

  }


  private def initializeDependencyDownloader(config: Config) = {
    val depsDir = {
      if(config.hasPath("deps_dir") && Files.exists(Paths.get(config.getString("deps_dir")))) {
        config.getString("deps_dir")
      } else {
        FileUtils.createManagedTempDirectory("toree_add_deps").getAbsolutePath
      }
    }

    val dependencyDownloader = new CoursierDependencyDownloader
    dependencyDownloader.setDownloadDirectory(
      new File(depsDir)
    )

    if (config.hasPath("default_repositories")) {
      val repository = config.getStringList("default_repositories").asScala.toList

      val credentials = if (config.hasPath("default_repository_credentials")) {
        config.getStringList("default_repository_credentials").asScala.toList
      } else Nil

      dependencyDownloader.resolveRepositoriesAndCredentials(repository, credentials)
        .foreach{case (u, c) => dependencyDownloader.addMavenRepository(u, c)}
    }

    dependencyDownloader
  }

  protected def initializeResponseMap(): collection.mutable.Map[String, ActorRef] =
    new ConcurrentHashMap[String, ActorRef]().asScala

  private def initializeKernel(
    config: Config,
    actorLoader: ActorLoader,
    interpreterManager: InterpreterManager,
    dependencyDownloader: DependencyDownloader) = {

    //kernel has a dependency on ScalaInterpreter to get the ClassServerURI for the SparkConf
    //we need to pre-start the ScalaInterpreter
//    val scalaInterpreter = interpreterManager.interpreters("Scala")
//    scalaInterpreter.start()

    val kernel = new Kernel(
      config,
      actorLoader,
      interpreterManager,
      dependencyDownloader
    ){
      override protected[toree] def createSparkConf(conf: SparkConf) = {
        val theConf = super.createSparkConf(conf)

        // TODO: Move SparkIMain to private and insert in a different way
        logger.warn("Locked to Scala interpreter with SparkIMain until decoupled!")

        // TODO: Construct class server outside of SparkIMain
        logger.warn("Unable to control initialization of REPL class server!")

        theConf
      }
    }

    kernel
  }
}
