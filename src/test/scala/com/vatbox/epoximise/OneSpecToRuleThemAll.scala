package com.vatbox.epoximise

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by talg on 24/11/2016.
  */
class OneSpecToRuleThemAll extends FreeSpecLike with GeneratorDrivenPropertyChecks with ScalaFutures with Matchers with BeforeAndAfterAll{
  val dao = SimpleDao
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(10,Seconds),Span(100, Millis))

  "Generate random objects and insert them to DB" in {
    var buffer = mutable.Buffer[UpperLevel]()
    forAll(Generators.upperLevelGen, minSuccessful(100)) { obj =>
      val future = dao.insert(obj)
      future.map(_ => buffer += obj).futureValue
    }

    val seq = dao.find[UpperLevel]().futureValue

    seq should contain theSameElementsAs buffer
  }

  override protected def beforeAll(): Unit = {
    dao.clean().futureValue
  }
}
