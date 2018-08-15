resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("scala-js-snapshots", 
    url("http://repo.scala-js.org/repo/snapshots/"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
