package com.gilt

import scala.util.Try

case class Parameters(accessKey: Option[String] = None,
                      secretKey: Option[String] = None,
                      streamName: Option[String] = None,
                      roleArn: Option[String] = None,
                      fileName: Option[String] = None,
                      jsonData: Option[String] = None) {

  def isValid: Either[String, Unit] = {
    if (accessKey.nonEmpty || secretKey.nonEmpty || streamName.nonEmpty || roleArn.nonEmpty) {
      if (fileName.nonEmpty || jsonData.nonEmpty) {
        Left("If one stream is configured with accessKey/secretKey/streamName/roleArn, then you must not specify neither file nor json data.")
      } else {
        if (accessKey.nonEmpty && secretKey.isEmpty || accessKey.isEmpty && secretKey.nonEmpty) {
          Left("AccessKey and SecretKey must both be provided, or both omitted.")
        } else {
          if (streamName.isEmpty) {
            Left("To specify a single stream, you must provide at least the streamName.")
          } else {
            Right()
          }
        }
      }
    } else {
      if (fileName.nonEmpty && jsonData.nonEmpty) {
        Left("To specify multiple streams, you must provide either a file or json data, but not both")
      } else {
        if (fileName.isEmpty && jsonData.isEmpty) {
          Left("You must provide at least one configuration.")
        } else {
          Right()
        }
      }
    }
  }

  def toClientConfig: ClientConfiguration = {
    if (fileName.nonEmpty) {
      ClientConfiguration.fromFile(fileName.get)
    } else {
      if (jsonData.nonEmpty) {
        ClientConfiguration.fromJson(jsonData.get)
      } else {
        ClientConfiguration(accessKey, secretKey, roleArn, Seq(StreamConfiguration(streamName.get, None)))
      }
    }
  }
}

object Parameters {

  private[this] def parser(appName: String, versionStr: String) = new scopt.OptionParser[Parameters](appName) {

    head(appName, versionStr)

    opt[String]('a', "accessKey")
      .valueName("<key>")
      .text("Specify the access key to use. Optional, must be accompanied by a secretKey.")
      .action { (value, params) => params.copy(accessKey = Some(value)) }

    opt[String]('s', "secretKey")
      .valueName("<key>")
      .text("Specify the secret key to use. Optional, must be accompanied by an accessKey.")
      .action { (value, params) => params.copy(secretKey = Some(value)) }

    opt[String]('k', "kinesisStream")
      .valueName("<stream name>")
      .text("Specify the name of the Kinesis stream. Mandatory, unless you use -f or -d.")
      .action { (value, params) => params.copy(streamName = Some(value)) }

    opt[String]('r', "roleArn")
      .valueName("<role arn>")
      .text("Specify the IAM role to assume before reading from the Kinesis stream. Optional.")
      .action { (value, params) => params.copy(roleArn = Some(value)) }

    opt[String]('f', "file")
      .valueName("<file>")
      .text("Specify a configuration file. No other options can be used when this is specified.")
      .action { (value, params) => params.copy(fileName = Some(value)) }

    opt[String]('d', "data")
      .valueName("<json data>")
      .text("Specify configuration as JSON. No other options can be used when this is specified.")
      .action { (value, params) => params.copy(jsonData = Some(value))
      }

    checkConfig { params =>
      params.isValid match {
        case Left(message) => failure(message)
        case Right(_) => success
      }
    }

    note("You can specify configuration for a single stream by passing in a streamName and optional accessKey, secretKey, and roleArn.\n" +
      "For multiple streams, you must pass in a configuration file or a JSON structure with the configuration parameters.")

    help("help").text("Prints this usage text")
  }


  def toConfiguration(args: Array[String])(body: ClientConfiguration => Any) = {
    parser("kinesis-consumer", "1.0").parse(args, Parameters()) match {
      case Some(params) =>
        Try(params.toClientConfig) map { clientConfig =>
          if (clientConfig.streams.isEmpty) {
            println("ERROR: must provide at least one stream to consume from.")
          } else {
            body(clientConfig)
          }
        } recover {
          case e: Exception =>
            System.out.println(s"ERROR: ${e.getMessage}")
        }

      case None =>
      //
    }
  }
}