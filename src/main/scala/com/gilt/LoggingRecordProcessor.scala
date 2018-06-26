package com.gilt

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record
import com.gilt.gfc.logging.Loggable
import com.gilt.gfc.util.Retry

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class LoggingRecordProcessor(stream: String) extends IRecordProcessor with Loggable {

  private[this] var shardId: String = _
  private[this] var nextCheckpointTimeMillis: Long = -1
  private[this] val CheckpointIntervalInMillis = 10000L

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    if (shutdownInput.getShutdownReason == ShutdownReason.TERMINATE) {
      checkpoint(shutdownInput.getCheckpointer)
    }
    info(s"Record Processor stopped for stream $stream, shard $shardId.")
  }

  override def initialize(initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.getShardId
    info(s"Record Processor created for stream $stream, shard $shardId.")
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    val events = processRecordsInput.getRecords.asScala
    try {
      events foreach printEvent

      if (System.currentTimeMillis() > nextCheckpointTimeMillis) {
        checkpoint(processRecordsInput.getCheckpointer)
        nextCheckpointTimeMillis = System.currentTimeMillis() + CheckpointIntervalInMillis
      }
    } catch {
      case e: Exception =>
        warn(s"Failed to process events $events for stream $stream", e)
    }
  }

  private[this] def printEvent(event: Record): Unit = {
    val message = new String(event.getData.array(), "UTF-8")
    info(s"[$stream] $message")
  }

  private[this] def checkpoint(checkpointer: IRecordProcessorCheckpointer): Unit = {
    Retry.retryWithExponentialDelay(maxRetryTimes = 10, initialDelay = 1.second) {
      checkpointer.checkpoint()
    }(_ => ())
  }
}
