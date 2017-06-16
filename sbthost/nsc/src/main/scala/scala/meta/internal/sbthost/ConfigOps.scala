package scala.meta.internal.sbthost

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

case class SbthostConfig(sourceroot: Path, targetroot: Path) {
  def target = targetroot.resolve("META-INF").resolve("semanticdb")
  def relativePath(path: Path) = sourceroot.relativize(path)
  def semanticdbPath(relativePath: Path) = {
    val sibling = relativePath.getFileName.toString + ".semanticdb"
    target
      .resolve(relativePath)
      .resolveSibling(sibling)
      .toAbsolutePath
  }
}

trait ConfigOps { self: DatabaseOps =>
  def workingDirectory = Paths.get(sys.props("user.dir")).normalize()

  def targetroot = {
    /* Note that `sbt.Eval` does not set the output dirs when initializing global,
     * but does it per-run because the output directory may change. This will always
     * return the working directory because config is initialized when global is. */
    val default = Paths
      .get(
        g.settings.outputDirs.getSingleOutput
          .map(_.file.toURI)
          .getOrElse(new File(g.settings.d.value).getAbsoluteFile.toURI))
      .normalize()

    // Imitate what classesDirectory does for sbt 0.13.x for consistency
    if (default == workingDirectory) {
      // Sbthost is only meant to be used for 2.10, so this is safe
      workingDirectory.resolve(Paths.get("target", "scala-2.10", "classes"))
    } else default
  }
  var config = SbthostConfig(
    sourceroot = workingDirectory.toAbsolutePath,
    targetroot = targetroot.toAbsolutePath
  )
}
