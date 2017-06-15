// Expressions
name := "basic"
organization := "me.vican.jorge"

// Definitions
val p1 = project

lazy val copyAll = taskKey[Unit]("")
copyAll in Global := {
  val tempDir = file("/tmp/hehe")
  streams.value.log.info("Copying all stuff...")
  IO.copyDirectory(baseDirectory.value, tempDir, overwrite = true)
}
