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
  // Note that this is a hook to copy semanticdb generated in the metabuild after it's been loaded
  Option(sbtHostProperties.getProperty("target")) match {
    case Some(targetClassesDir) =>
      val logger = sLog.value
      val target = new File(targetClassesDir)
      val toCopy = (classDirectory in Compile).value
      val allDbs = (toCopy ** "*.semanticdb").get.map(_.getAbsolutePath)
      if (allDbs.nonEmpty) {
        logger.info(s"The following db files were found: ${allDbs.mkString(",")}")
        logger.info(s"Copying them to ${target.getAbsolutePath}.")
        IO.copyDirectory(toCopy, target, overwrite = true)
      }
    case None => sys.error(s"Missing 'target' system property in $configFilepath")
  }
}
