package com.gilt

import java.net.InetAddress
import java.util.UUID

import com.amazonaws.auth.{AWSCredentialsProvider, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory

class KinesisConsumer(streamName: String, credentialsProvider: AWSCredentialsProvider, iamRoleArnOpt: Option[String]) extends IRecordProcessorFactory {

  val uuid = UUID.randomUUID()
  val appName = s"kinesisConsumer-$streamName-$uuid"
  val workerId = s"${InetAddress.getLocalHost.getCanonicalHostName}:$uuid"

  val dynamoCredentialsProvider = credentialsProvider

  val kinesisCredentialsProvider = iamRoleArnOpt map { iamRoleArn =>
    new STSAssumeRoleSessionCredentialsProvider(credentialsProvider, iamRoleArn, appName.take(64))
  } getOrElse credentialsProvider

  val cloudwatchCredentialsProvider = credentialsProvider

  val kclConfig = new KinesisClientLibConfiguration(
      appName,
      streamName,
      kinesisCredentialsProvider,
      dynamoCredentialsProvider,
      cloudwatchCredentialsProvider,
      workerId)
      .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)

  override def createProcessor(): IRecordProcessor = new LoggingRecordProcessor(streamName)

  def instance(): Worker = new Worker.Builder()
    .recordProcessorFactory(this)
    .metricsFactory(new NullMetricsFactory)
    .config(kclConfig)
    .build()
}
