import de.choffmeister.microserviceutils.plugin.MicroserviceUtilsPluginVersion

val akkaVersion = MicroserviceUtilsPluginVersion.akka
val akkaHttpVersion = MicroserviceUtilsPluginVersion.akkaHttp
val lagomVersion = "1.4.11"
val jjwtVersion = "0.10.6"
val slickVersion = "3.3.2"

scalaVersion in ThisBuild := "2.12.10"

lazy val server = Project("server", base = file("server"))
  .settings(
    name := "dochub",
    organization := "de.choffmeister",
    resolvers += Resolver.bintrayRepo("choffmeister", "maven"),
    libraryDependencies ++= Seq(
      microserviceUtils,
      microserviceUtilsApis,
      microserviceUtilsTestkit % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.github.tminglei" %% "slick-pg" % "0.18.0",
      "com.github.tminglei" %% "slick-pg_play-json" % "0.18.0",
      "com.softwaremill.macwire" %% "macros" % Versions.macwireVersion % Provided,
      "com.typesafe.akka" %% "akka-slf4j" % Versions.akkaVersion,
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
      "commons-codec" % "commons-codec" % "1.13",
      "de.heikoseeberger" %% "akka-http-play-json" % "1.22.0",
      "io.jsonwebtoken" % "jjwt-api" % Versions.jjwtVersion,
      "io.jsonwebtoken" % "jjwt-impl" % Versions.jjwtVersion % Runtime,
      "io.jsonwebtoken" % "jjwt-jackson" % Versions.jjwtVersion % Runtime,
      "org.postgresql" % "postgresql" % "42.2.5",
      "org.apache.mina" % "mina-core" % "2.1.3",
      "org.apache.ftpserver" % "ftplet-api" % "1.1.1",
      "org.apache.ftpserver" % "ftpserver-core" % "1.1.1"
    ),
    packageName in Docker := "dochub",
    dockerRepository in Docker := Some("docker.pkg.home.choffmeister.de/dochub"),
    mappings in Docker := {
      def listFiles(base: File, cur: File): Seq[(File, String)] = IO.listFiles(cur).toSeq.flatMap {
        case dir if dir.isDirectory => (dir, IO.relativize(base, dir).get) +: listFiles(base, dir)
        case file                   => (file, IO.relativize(base, file).get) :: Nil
      }

      val log = streams.value.log
      val dockerName = (name in Docker).value
      val dockerMappings = (mappings in Docker).value
      val webDir = baseDirectory.value.getParentFile / "web"
      val webBuildDir = webDir / "build"

      if (!webBuildDir.isDirectory) {
        import scala.sys.process.Process
        log.info("Building web folder")
        Process("yarn" :: "install" :: "--pure-lockfile" :: Nil, cwd = webDir) !! log
        Process("yarn" :: "build" :: Nil, cwd = webDir) !! log
      } else {
        log.info("Web build folder exists. Skipping")
      }

      (dockerMappings :+ (webBuildDir, "web")) ++ listFiles(webBuildDir, webBuildDir)
        .map(m => (m._1, s"/opt/$dockerName/web/" + m._2))
    },
    scmInfo := Some(ScmInfo(url("https://github.com/choffmeister/dochub"), "git@github.com:choffmeister/dochub.git")),
    git.remoteRepo := scmInfo.value.get.connection,
    paradoxProperties ++= Map(
      "snip.base.base_dir" -> s"${baseDirectory.value}",
      "snip.conf.base_dir" -> s"${baseDirectory.value}/src/universal/conf",
      "snip.src.base_dir" -> s"${baseDirectory.value}/src/main/scala/de/choffmeister/dochub/",
      "scaladoc.de.choffmeister.dochub.base_url" -> "/api"
    ),
    parallelExecution in Test := false
  )
  .enablePlugins(GitVersioning, DockerImagePlugin, DocumentationPlugin)

lazy val root = Project("root", base = file("."))
  .settings(name := "dochub-root", skip in publish := true)
  .aggregate(server)
