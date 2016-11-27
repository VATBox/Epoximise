package com.vatbox.epoximise

import java.time.LocalDateTime
import java.util.UUID

import org.bson.types.ObjectId
import org.scalacheck.{Arbitrary, Gen}

/**
  * Created by talg on 24/11/2016.
  */
object Generators {
  def upperLevelGen: Gen[UpperLevel] = for{
    i <- Arbitrary.arbInt.arbitrary
    b <- Arbitrary.arbBool.arbitrary
    s <- Gen.listOfN(10,Gen.alphaNumChar).map(_.mkString)
    o <- Gen.option(Arbitrary.arbBool.arbitrary)
    d <- Arbitrary.arbDouble.arbitrary
    l <- Arbitrary.arbLong.arbitrary
//    date <- Gen.choose(1479991486000L,1479981486000L).map(new Date(_))
    uuid <- Gen.uuid
    set <- Gen.choose(0,5).flatMap(size => Gen.listOfN(size,embeddedGen)).map(_.toSet)
  } yield UpperLevel(
    i,
    b,
    s,
    o,
    d,
    l,
//    date,
    ObjectId.get(),
    uuid,
    set
  )

  def embeddedGen : Gen[Embedded] = for {
    ii <- Arbitrary.arbInt.arbitrary
    bb <- Arbitrary.arbBool.arbitrary
    arr <- Gen.choose(0,10).flatMap(size => Gen.listOfN(size, Arbitrary.arbInt.arbitrary))
    lDate <- Gen.choose(10000,100000).map { sec =>
      val time = LocalDateTime.now().minusSeconds(sec)
      val nanos = time.getNano
      time.minusNanos(nanos)
    }
  } yield Embedded(
    ii,
    bb,
    arr,
    lDate
  )
}

case class UpperLevel(
                       i: Int,
                       b: Boolean,
                       s: String,
                       o: Option[Boolean],
                       d: Double,
                       l: Long,
//                       date: Date,
                       objectId: ObjectId,
                       uuid: UUID,
                       set: Set[Embedded]
                     )

case class Embedded(ii: Int, bb: Boolean, arr: List[Int], lclDate: LocalDateTime)