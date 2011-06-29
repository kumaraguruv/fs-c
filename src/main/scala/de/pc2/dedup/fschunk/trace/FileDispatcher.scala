package de.pc2.dedup.fschunk.trace

import java.io.File
import scala.actors.Actor
import scala.actors.Actor._
import de.pc2.dedup.chunker._
import scala.actors.Exit
import java.util.concurrent._
import de.pc2.dedup.util._
import java.util.concurrent.atomic.AtomicLong
import de.pc2.dedup.fschunk.handler.FileDataHandler

trait FileDispatcher {
  def dispatch(f: File, path: String, isDir: Boolean, label: Option[String])
}

/**
 * Dispatches files to a number of file processors that chunk the file contents
 */
class ThreadPoolFileDispatcher(processorNum: Int,
  chunker: Seq[(Chunker, List[FileDataHandler])],
  useDefaultIgnores: Boolean,
  followSymlinks: Boolean,
  progressHandler: (de.pc2.dedup.chunker.File) => Unit) extends FileDispatcher with Log {
  var lock: AnyRef = new Object()
  var finished: Boolean = false
  val startTime = System.currentTimeMillis()

  def shouldShutdown: Boolean = {
    val dircount = direxecutor.getActiveCount() + direxecutor.getQueue().size()
    val filecount = fileexecutor.getActiveCount() + fileexecutor.getQueue().size()
    return dircount + filecount <= 1
  }

  class DirectoryDispatcherThreadPoolExecutor(dispatcher: ThreadPoolFileDispatcher) extends ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
    new ArrayBlockingQueue[Runnable](processorNum * 2),
    new ThreadPoolExecutor.CallerRunsPolicy()) {
    logger.debug("Created directory threadpool with at most %d threads".format(4))
    override def afterExecute(r: Runnable, t: Throwable) {
      if (shouldShutdown) {
        dispatcher.executorFinished()
      }
    }
  }
  class FileDispatcherThreadPoolExecutor(dispatcher: ThreadPoolFileDispatcher) extends ThreadPoolExecutor(processorNum, processorNum, 30, TimeUnit.SECONDS,
    new ArrayBlockingQueue[Runnable](processorNum * 1024),
    new ThreadPoolExecutor.CallerRunsPolicy()) {
    logger.debug("Created file threadpool with at most %d threads".format(processorNum))
    override def afterExecute(r: Runnable, t: Throwable) {
      if (shouldShutdown) {
        dispatcher.executorFinished()
      }
    }
  }

  val fileexecutor = new FileDispatcherThreadPoolExecutor(this)
  val direxecutor = new DirectoryDispatcherThreadPoolExecutor(this)

  def dispatch(f: File, path: String, isDir: Boolean, label: Option[String]) {
    if (logger.isDebugEnabled) {
      logger.debug("Dispatch " + f)
    }
    if (isDir) {
      direxecutor.execute(new DirectoryProcessor(f, label, useDefaultIgnores, this, followSymlinks))
    } else {
      fileexecutor.execute(new FileProcessor(f, path, label, chunker, 4 * 1024 * 1024, progressHandler))
    }
  }

  def waitUntilFinished() {
    lock.synchronized {
      while (!finished) {
        lock.wait()
      }
    }
  }

  def executorFinished() {
    direxecutor.shutdown()
    fileexecutor.shutdown()

    lock.synchronized {
      finished = true
      lock.notifyAll()
    }
  }

  def report() {
    val secs = ((System.currentTimeMillis() - startTime) / 1000)
    if (secs > 0) {
      val mbs = FileProcessor.totalRead.get() / secs
      logger.info("Files total: %d, data %s (%s/s), active: %d, scheduled: %d, pool %d, directories total: %d, active: %d, scheduled %d, pool %d".format(
        FileProcessor.totalCount.get(),
        StorageUnit(FileProcessor.totalRead.get()),
        StorageUnit(mbs),
        FileProcessor.activeCount.get(),
        fileexecutor.getQueue().size,
        fileexecutor.getPoolSize,
        DirectoryProcessor.totalCount.get(),
        DirectoryProcessor.activeCount.get(),
        direxecutor.getQueue().size,
        direxecutor.getPoolSize))
    }
  }
}
