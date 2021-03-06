package com.vatbox.epoximise

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import org.bson.BsonValue
import org.json4s.{Extraction, Formats, JValue}
import org.mongodb.scala.Document

import scala.util.Try

/**
  * Created by talg on 24/11/2016.
  */
trait Epoximise { self =>
  protected val parser: EpoximiseParser
  protected val serializer: EpoximiseSerializer

  implicit def convertToJvalue(any: Any)(implicit formats: Formats): JValue = Extraction.decompose(any)

  implicit def toBsonValue[A <: AnyRef](canBeJvalue: A)(implicit ev: A => JValue, formats: Formats): BsonValue = parser.parse(canBeJvalue)

  implicit def fromBsonValue[B](bson: B)(implicit e: B => Document, formats: Formats): JValue = serializer.serialize(bson.toBsonDocument)

  implicit def toBsonDocument[A <: AnyRef](any: A)(implicit ev: A => JValue, formats: Formats): Document = {
    val javaDoc = toBsonValue(any).asDocument()
    Document(javaDoc)
  }

  implicit def fromJvalueToOptionValue[A:Manifest](jValue: JValue)(implicit formats: Formats): Option[A] = {
    Extraction.extractOpt[A](jValue)
  }

  implicit def fromJvalueToValue[A:Manifest](jValue: JValue)(implicit formats: Formats): A = {
    Extraction.extract[A](jValue)
  }

  implicit def fromDocumentToOptionValue[A: Manifest](document: Document)(implicit formats: Formats): Option[A] = {
    Try(self.fromBsonValue(document)).map(fromJvalueToValue[A]).toOption
  }

  implicit def fromDocumentToValue[A: Manifest](document: Document)(implicit formats: Formats): A = {
    self.fromBsonValue(document)
  }

  implicit class EpoximiseHelperFromBson(val document: Document) {
    def fromBsonValue(implicit formats: Formats): JValue = self.fromBsonValue(document)
    def extract[A: Manifest](implicit formats: Formats) : A = document
    def extractOpt[A: Manifest](implicit formats: Formats) : Option[A] = document
  }
}

case class EpoximiseBuilder(
                             private val localDateTimeFormatter: DateTimeFormatter = DefaultDateTimeFormatter,
                             private val optimizeBigInt: Boolean = true,
                             private val zoneOffset: ZoneOffset = DefaultZoneOffSet
                           ) {
  def withLocalDateTimeFormatter(formatter: DateTimeFormatter): EpoximiseBuilder = copy(localDateTimeFormatter = formatter)

  /**
    * If it fits into Long/Int it will be created as one accordingly
    */
  def dontOptimizeBigInteger: EpoximiseBuilder = copy(optimizeBigInt = false)

  def withZoneOffSet(offset: ZoneOffset): EpoximiseBuilder = copy(zoneOffset = offset)

  def build() = new Epoximise {
    override protected val parser: EpoximiseParser = EpoximiseParser(localDateTimeFormatter, optimizeBigInt, zoneOffset)
    override protected val serializer: EpoximiseSerializer = EpoximiseSerializer(localDateTimeFormatter, zoneOffset)
  }
}

object DefaultEpoximiseInstance extends Epoximise {
  override protected val parser: EpoximiseParser = EpoximiseParser(DefaultDateTimeFormatter, optimizeBigInt = true, DefaultZoneOffSet)
  override protected val serializer: EpoximiseSerializer = EpoximiseSerializer(DefaultDateTimeFormatter, DefaultZoneOffSet)
}