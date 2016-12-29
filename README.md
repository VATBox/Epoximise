# Epoximise - library to serialize/deserialize MongoDB (ver 3.0+) [BsonValue](http://mongodb.github.io/mongo-java-driver/3.4/javadoc/org/bson/BsonValue.html) 
[![CircleCI](https://circleci.com/gh/VATBox/Epoximise.svg?style=svg)](https://circleci.com/gh/VATBox/Epoximise)

```
    libraryDependencies += "com.vatbox" %% "epoximise" % "1.0.2"
```

## Example is the best way to explain what it does.
 * Let's create 2 scala classes first, they are pretty generic:
    
    ```scala
    case class UpperLevel(
                           i: Int,
                           b: Boolean,
                           s: String,
                           o: Option[Boolean],
                           d: Double,
                           l: Long,
                           objectId: ObjectId,
                           uuid: UUID,
                           set: Set[Embedded]
                         )
    case class Embedded(ii: Int, bb: Boolean, arr: List[Int], lclDate: LocalDateTime)
    ```
    We will also create some __random__ objects using [scalacheck](https://www.scalacheck.org) generators ([example](/src/test/scala/com/vatbox/epoximise/Generators.scala)).
 * Now that we have our objects let's persist them to MongoDB using [MongoDB scala driver](https://docs.mongodb.com/ecosystem/drivers/scala/). 
 For that we will create a really simple DAO with the following methods:
    - Insert __ANY__(almost) object = `def insert[A <: AnyRef](entity : A): Future[Completed]`
    - Find All = `def find[A: Manifest](): Future[Seq[A]]`
 * In order to persist our almost __ANY__ object into DB we will need this library:
    
    ```scala
    val epox: Epoximise = EpoximiseBuilder().build()
    import epox._
    ```
    That's it.
 * This is how our DAO looks like (full example [here](/src/test/scala/com/vatbox/epoximise/OneSpecToRuleThemAll.scala)):
    
    ```scala
    object SimpleDao {
      val collection: MongoCollection[Document] = db.getCollection("objects")
      val epox: Epoximise = EpoximiseBuilder().build()
      import epox._
    
      def insert[A <: AnyRef](entity : A): Future[Completed] = collection.insertOne(entity).head()
    
      def find[A: Manifest](): Future[Seq[A]] = collection.find().map(_.extract[A]).toFuture()
    }
    ```
 * Now what's left for us is to use it:
    
    ```scala
    class OneSpecToRuleThemAll extends AsyncFreeSpecLike with ScalaFutures with GeneratorDrivenPropertyChecks with Matchers with BeforeAndAfterAll{
      val dao = SimpleDao
    
      "Generate random objects and insert them to DB" in {
        var buffer = mutable.Buffer[UpperLevel]()
        forAll(Generators.upperLevelGen,MinSuccessful(100)) { obj =>
          val future = dao.insert(obj)
          future.map { _ =>
            buffer += obj
          }
        }
    
        dao.find[UpperLevel]().map { seq =>
          seq should contain theSameElementsAs buffer
        }
      }
    }
    ```
 * This is what is looks like in MongoDB:
    
    ```javascript
    db.objects.findOne()
    {
    	"_id" : ObjectId("586212cca36dbdbcee840c8d"),
    	"i" : 0,
    	"b" : false,
    	"s" : "qncavbapct",
    	"d" : 1.8429734143155635e+270,
    	"l" : -1,
    	"objectId" : ObjectId("586212cca36dbdbcee840c8c"),
    	"uuid" : BinData(4,"XqHqQgSgTHyubkEfZtGlog=="),
    	"set" : [
    		{
    			"ii" : 165273565,
    			"bb" : true,
    			"arr" : [
    				-2147483648,
    				-1,
    				-1422343589,
    				-329538388,
    				-109958632,
    				-1753241949
    			],
    			"lclDate" : ISODate("2016-12-26T17:21:33Z")
    		},
    		{
    			"ii" : -2147483648,
    			"bb" : false,
    			"arr" : [
    				-1730771104,
    				0,
    				192933199,
    				-1499973888,
    				-1290175961,
    				2147483647,
    				-2147483648,
    				-1,
    				-1688550900
    			],
    			"lclDate" : ISODate("2016-12-26T17:44:52Z")
    		},
    		{
    			"ii" : 0,
    			"bb" : true,
    			"arr" : [
    				-1,
    				-584284726,
    				-2147483648,
    				1,
    				-1547982927,
    				-2147483648,
    				1600623380
    			],
    			"lclDate" : ISODate("2016-12-26T07:15:51Z")
    		},
    		{
    			"ii" : -1,
    			"bb" : false,
    			"arr" : [
    				0,
    				-2147483648,
    				0,
    				2114507037,
    				-2147483648,
    				0,
    				-1938015543,
    				0
    			],
    			"lclDate" : ISODate("2016-12-26T10:06:59Z")
    		},
    		{
    			"ii" : 367712630,
    			"bb" : true,
    			"arr" : [
    				2147483647,
    				2147483647,
    				-2038381930
    			],
    			"lclDate" : ISODate("2016-12-26T10:28:07Z")
    		}
    	]
    }
    ```
## This library is inspired by [Json4s](https://github.com/json4s/json4s) old mongo driver support which means you will need Json4s in order to use it.
 * Dependencies you will need to add are:
  
  ```scala
    "org.mongodb.scala" %% "mongo-scala-driver" % "1.2.1",
    "org.json4s" %% "json4s-core" % "3.5.0"
  ```
 * You will also need to have [Formats](https://github.com/json4s/json4s/blob/3.6/core/src/main/scala/org/json4s/Formats.scala) implicitly available when using this library:
    
    ```scala
     implicit val formats: Formats
    ```
    There are several useful serializers/deserializers included in this library make sure you include them if needed.
    
    ```scala
     object ObjectIdSerializer extends Serializer[ObjectId]
     object UUIDSerializer extends Serializer[UUID]
     // Date* serializers - pick one since you can't use both when deserializing from MongoDB 
     case class LocalDateTimeSerializer(localDateTimeFormatter: DateTimeFormatter) extends Serializer[LocalDateTime]
     object DateSerializer extends Serializer[Date]
  
     implicit val formats: Formats = DefaultFormats + ObjectIdSerializer + LocalDateTimeSerializer() + UUIDSerializer
    ```
