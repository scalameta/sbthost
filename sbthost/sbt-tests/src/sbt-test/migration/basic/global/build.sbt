val configFilepath = System.getProperty("sbthost.config")
val sbtHostProperties = {
  import java.io.FileInputStream
  import java.util.Properties
  require(configFilepath != null, "Missing sbthost.config property, did you pass it to scripted?")
  val properties = new Properties()
  properties.load(new FileInputStream(new File(configFilepath)))
  properties
}

scalacOptions := {
  Option(sbtHostProperties.getProperty("scalac")) match {
    case Some(sbtHostScalacOptions) =>
      streams.value.log.info("Instrumenting scalac with sbthost scalac flags.")
      Keys.scalacOptions.value ++ sbtHostScalacOptions.split("\007")
    case None => sys.error(s"Missing 'scalac' system property in $configFilepath")
  }
}

initialize := {
  import scala.collection.JavaConversions._
  Option(sbtHostProperties.getProperty("target")) match {
    case Some(targetDir) =>
      val allDbs = (baseDirectory.value ** "*.semanticdb").get
      val allFilepaths = allDbs.map(_.getAbsolutePath)
      sLog.value.info(s"Copying the db files to the target directory:\n${allFilepaths}.")
      IO.copy(allDbs.map(db => db -> (new File(targetDir) / db.getName)))
    case None => sys.error(s"Missing 'target' system property in $configFilepath")
  }
}
