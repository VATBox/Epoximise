package com.vatbox.epoximise

import org.json4s.{DefaultFormats, Formats}
import org.mongodb.scala.result.DeleteResult
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by talg on 24/11/2016.
  */
object SimpleDao {
  implicit val formats: Formats = DefaultFormats + ObjectIdSerializer + LocalDateTimeSerializer(DefaultDateTimeFormatter) + DateSerializer + UUIDSerializer
  val mongoClient = MongoClient()
  val db: MongoDatabase = mongoClient.getDatabase("test_epoximise")
  val collection: MongoCollection[Document] = db.getCollection("objects")
  val epox: Epoximise = EpoximiseBuilder().build()
  import epox._

  def insert[A <: AnyRef](entity : A): Future[Completed] = {
    collection.insertOne(entity).toFuture().map(_.head)
  }

  def find[A: Manifest](): Future[Seq[A]] = {
    /**
        @note If the Observable is large then this will consume lots of memory!
          If the underlying Observable is infinite this Observable will never complete.
      */
    collection.find().toFuture().map( seq =>
      seq.map(_.extract[A])
    )
  }

  def clean(): Future[DeleteResult] = {
    collection.deleteMany(Document.empty).toFuture().map(_.head)
  }
}
