# First, publish locally (not yet public maven repository)

```
sbt publishLocal
```

# Add in build.sbt

```
libraryDependency += "com.iz2use" %% "scalajs-indexeddb" % "0.0.1-SNAPSHOT"
```

# How to use

Define object stores :

```

@ScalaJSDefined
final class Task(
  val id: String = js.native,
  val title: String = js.native,
  val created: Date = js.native,
  val done: Option[Date] = js.native,
  val tags: js.Array[String] = js.native
) extends js.Object

object Tasks extends ObjectStore[Task, String]("tasks", 1) {
  override def keyOf(item: Task) : String = item.id
  override def write(item: Task) :js.Object = item
  override def read(item: js.Object): Task = item.asInstanceOf[Task]
  val byCreated = secondaryKey[Date]("created", KeyOptions.None, "created")
  val byTitle = secondaryKey[String]("title", KeyOptions.None, "title")
  val byTag = secondaryKey[String]("tags", KeyOptions.MultiEntry, "tags")

  def restFromDate(start: Date)(implicit db: IndexedDB) = 
    range(byCreated)(Range(start,true))
}

```


Define an object that extends IndexedDB :

```
object DB extends IndexedDB {
  override def objectStores: Seq[ObjectStore[_, _]] = Seq(Tasks)

  override def notifyUpdate(): Unit = {}

  override val version = 1
}
```


```
Tasks.put(new Task("A", "Hello", new Date(), None, js.Array("Important") )) onComplete {
  case Future(_) =>
  case Failure(NonFatal(e)) =>
}

Tasks.restFromDate(new Date().minusDays(7)) onComplete { 
  case Success(tasks) =>
  for { task <- tasks} {
  }
  case Failure( NonFatal(e)) =>
}
```