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
    val default = Paths
      .get(
        g.settings.outputDirs.getSingleOutput
          .map(_.file.toURI)
          .getOrElse(new File(g.settings.d.value).getAbsoluteFile.toURI))
      .normalize()
    // It seems the default points to the working directory when compiling
    // sbt builds.
    if (default == workingDirectory) workingDirectory.resolve("target")
    else default
  }
  var config = SbthostConfig(
    sourceroot = workingDirectory.toAbsolutePath,
    targetroot = targetroot.toAbsolutePath
  )
}
