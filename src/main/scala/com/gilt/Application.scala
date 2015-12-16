package com.gilt

object Application extends App {

  Parameters.toConfiguration(args) { clientConfig =>

    val threads = clientConfig.streams.map { streamConfig =>
      val streamRoleArn = streamConfig.roleArnOpt orElse clientConfig.defaultRole

      new Thread(
        new KinesisConsumer(
          streamConfig.streamName,
          clientConfig.getCredentialsProvider,
          streamRoleArn
        ).instance()
      )
    }

    threads foreach(_.start())
    threads foreach(_.join())
  }
}
