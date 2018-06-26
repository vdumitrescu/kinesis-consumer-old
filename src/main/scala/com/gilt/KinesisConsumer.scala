package com.gilt

import java.net.InetAddress
import java.util.UUID

import com.amazonaws.auth.{AWSCredentialsProvider, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory

class KinesisConsumer(streamName: String, credentialsProvider: AWSCredentialsProvider, iamRoleArnOpt: Option[String]) extends IRecordProcessorFactory {

  private[this] val uuid = UUID.randomUUID()
  private[this] val appName = s"kinesisConsumer-$streamName-$uuid"
  private[this] val workerId = s"${InetAddress.getLocalHost.getCanonicalHostName}:$uuid"

  private[this] val dynamoCredentialsProvider = credentialsProvider

  private[this] val kinesisCredentialsProvider = iamRoleArnOpt map { iamRoleArn =>
    new STSAssumeRoleSessionCredentialsProvider(credentialsProvider, iamRoleArn, appName.take(64))
  } getOrElse credentialsProvider

  private[this] val cloudwatchCredentialsProvider = credentialsProvider

  private[this] val kclConfig = new KinesisClientLibConfiguration(
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
