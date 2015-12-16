package com.gilt

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, BasicAWSCredentials}
import com.amazonaws.internal.StaticCredentialsProvider
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, __}

import scala.io.Source
import scala.util.Try

case class StreamConfiguration(streamName: String, roleArnOpt: Option[String])
case class ClientConfiguration(accessKey: Option[String], secretKey: Option[String], defaultRole: Option[String], streams: Seq[StreamConfiguration]) {

  def getCredentialsProvider = (accessKey, secretKey) match {
    case (Some(ak), Some(sk)) => new StaticCredentialsProvider(new BasicAWSCredentials(ak, sk))
    case _ => new DefaultAWSCredentialsProviderChain
  }
}

object ClientConfiguration {
  implicit val streamReads: Reads[StreamConfiguration] = (
    (__ \ "name").read[String] and
    (__ \ "role").readNullable[String]
  )(StreamConfiguration.apply _)

  implicit val clientReads: Reads[ClientConfiguration] = (
    (__ \ "accessKey").readNullable[String] and
    (__ \ "secretKey").readNullable[String] and
    (__ \ "defaultRole").readNullable[String] and
    (__ \ "streams").read[Seq[StreamConfiguration]]
  )(ClientConfiguration.apply _)

  def fromJson(json: String): ClientConfiguration = Json.parse(json).asOpt[ClientConfiguration] getOrElse sys.error("Failed to parse configuration from json data.")
  def fromFile(file: String): ClientConfiguration = Try(Source.fromFile(file, "UTF-8").mkString).map(data => fromJson(data)).get
}
