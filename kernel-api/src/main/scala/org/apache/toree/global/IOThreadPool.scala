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
package org.apache.toree.global

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object IOThreadPool {

  private var executorService: ExecutionContext = null
  def init(classloader: ClassLoader): Unit = {
    val pool = Executors.newCachedThreadPool(
      new ThreadFactory {
        private val counter = new AtomicLong(0L)

        def newThread(r: Runnable) = {
          val thread = new Thread(r)
          thread.setName("io-thread-" +
            counter.getAndIncrement.toString)
          thread.setDaemon(true)
          thread.setContextClassLoader(classloader)
          thread
        }
      })
    executorService = ExecutionContext.fromExecutorService(pool)
  }

  def get: ExecutionContext = {
    if (executorService == null) {
      throw new Exception("IOPool not initialized")
    } else {
      executorService
    }
  }
}