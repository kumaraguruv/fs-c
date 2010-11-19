package de.pc2.dedup.fschunk.trace

import de.pc2.dedup.chunker._
import de.pc2.dedup.util.Log
import java.io.BufferedReader
import java.io.File 
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.lang.reflect.InvocationTargetException

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.Exit

class FileSystemChunking(listing: FileListingProvider, chunker: Chunker, handlers: List[Actor], maxThreads: Int, useDefaultIgnores: Boolean, followSymlinks: Boolean) extends Actor with Log {
	trapExit = true
	val dispatcher = new ThreadPoolFileDispatcher(maxThreads, chunker, handlers, useDefaultIgnores, followSymlinks).start()

	def report() {
		logger.debug("Queue: %d".format(
				mailboxSize))
	}

	def act() {   
		logger.debug("Start")

		link(dispatcher)
		handlers.foreach(h => link(h))

		// Append all files from listing to directory processor
		for(filename <- listing) {
			dispatcher ! getFile(filename) 
		}

		while(true) {
			receive {
			case Exit(actor,'normal) =>
			if(actor == dispatcher) {

				// If dispatcher is finished, stop all handlers and exit
				handlers.foreach(h => h ! Quit)
				logger.debug("Exit")
				exit()
			}
			logger.warn("Actor %s exited".format(actor))
			case Exit(actor, reason) =>
			  reason match {
			    case e: InvocationTargetException => logger.warn("Actor %s exited: %s".format(actor, e.getCause().getMessage()),e.getCause())
			    case e: Exception => logger.warn("Actor %s exited: %s".format(actor, e.getMessage()),e)
			    case r: Any => logger.warn("Actor %s exited with reason %s".format(actor, reason))
			  }
			case Report =>
			  report()
			dispatcher ! Report
			handlers.foreach(h => h ! Report)
			case msg: Any =>
				logger.warn("Unknown Message: " + msg)
			}
		}
	}

	def getFile(filename: String) : File = {
			if(filename.equals(".")) {
				try {
					new File(filename).getCanonicalFile()
				} catch { case e: IOException => 
				new File(filename)
				}
			} else {
				new File(filename)
			}
	}
}