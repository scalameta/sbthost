import com.trueaccord.scalapb.compiler.Version.scalapbVersion
import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

name := "sbthostRoot"
moduleName := "sbthostRoot"

lazy val nsc = project
  .in(file("sbthost/nsc"))
  .settings(
    moduleName := "sbthost-nsc",
    mergeSettings,
    publishableSettings,
    isFullCrossVersion,
    isScala210,
    description := "Compiler plugin to produce .semanticdb files for sbt builds.",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    protobufSettings
  )

lazy val runtime = project
  .in(file("sbthost/runtime"))
  .settings(
    sharedSettings,
    publishableSettings,
    moduleName := "sbthost-runtime",
    libraryDependencies += "org.scalameta" %% "scalameta" % scalametaVersion,
    description := "Library to patch broken .semanticdb files produced by sbthost-nsc."
  )

lazy val input = project
  .in(file("sbthost/input"))
  .settings(
    isScala210,
    nonPublishableSettings,
    scalacOptions ++= {
      val sbthostPlugin = Keys.`package`.in(nsc, Compile).value
      val sbthostPluginPath = sbthostPlugin.getAbsolutePath
      val dummy = "-Jdummy=" + sbthostPlugin.lastModified
      s"-Xplugin:$sbthostPluginPath" ::
        "-Xplugin-require:sbthost" ::
        "-Yrangepos" ::
        dummy ::
        Nil
    }
  )

lazy val tests = project
  .in(file("sbthost/tests"))
  .settings(
    sharedSettings,
    nonPublishableSettings,
    moduleName := "sbthost-tests",
    scalaVersion := scala212,
    description := "Tests for sbthost",
    test.in(Test) := test.in(Test).dependsOn(compile.in(input, Compile)).value,
    buildInfoPackage := "scala.meta.tests",
    buildInfoKeys := Seq[BuildInfoKey](
      "targetroot" -> classDirectory.in(input, Compile).value,
      "sourceroot" -> baseDirectory.in(ThisBuild).value
    )
  )
  .dependsOn(runtime)
  .enablePlugins(BuildInfoPlugin)

lazy val protobufSettings = Seq(
  PB.targets.in(Compile) := Seq(
    scalapb.gen(
      flatPackage = true // Don't append filename to package
    ) -> sourceManaged.in(Compile).value
  ),
  PB.protoSources.in(Compile) := Seq(file("sbthost/nsc/src/main/protobuf")),
  libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % scalapbVersion
)

lazy val mergeSettings = Def.settings(
  test.in(assembly) := {},
  logLevel.in(assembly) := Level.Error,
  assemblyJarName.in(assembly) :=
    name.value + "_" + scalaVersion.value + "-" + version.value + "-assembly.jar",
  assemblyOption.in(assembly) ~= { _.copy(includeScala = false) },
  Keys.`package`.in(Compile) := {
    val slimJar = Keys.`package`.in(Compile).value
    val fatJar =
      new File(crossTarget.value + "/" + assemblyJarName.in(assembly).value)
    val _ = assembly.value
    IO.copy(List(fatJar -> slimJar), overwrite = true)
    slimJar
  },
  packagedArtifact.in(Compile).in(packageBin) := {
    val temp = packagedArtifact.in(Compile).in(packageBin).value
    val (art, slimJar) = temp
    val fatJar =
      new File(crossTarget.value + "/" + assemblyJarName.in(assembly).value)
    val _ = assembly.value
    IO.copy(List(fatJar -> slimJar), overwrite = true)
    (art, slimJar)
  },
  pomPostProcess := { node =>
    new RuleTransformer(new RewriteRule {
      private def isScalametaDependency(node: XmlNode): Boolean = {
        def isArtifactId(node: XmlNode, fn: String => Boolean) =
          node.label == "artifactId" && fn(node.text)
        node.label == "dependency" && node.child.exists(child =>
          isArtifactId(child, _.startsWith("scalameta_")))
      }
      override def transform(node: XmlNode): XmlNodeSeq = node match {
        case e: Elem if isScalametaDependency(node) =>
          Comment("scalameta dependency has been merged into sbthost via sbt-assembly")
        case _ => node
      }
    }).transform(node).head
  }
)

lazy val scalametaVersion = "1.8.0"
lazy val scala210 = "2.10.6"
lazy val scala211 = "2.11.11"
lazy val scala212 = "2.12.2"

lazy val isScala210 = Seq(
  scalaVersion := scala210,
  crossScalaVersions := List(scala210)
)

lazy val sharedSettings = Def.settings(
  scalaVersion := scala212,
  crossScalaVersions := List(scala212, scala211),
  organization := "org.scalameta",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  updateOptions := updateOptions.value.withCachedResolution(true),
  triggeredMessage.in(ThisBuild) := Watched.clearWhenTriggered
)

lazy val nonPublishableSettings = Seq(
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in packageDoc := false,
  sources in (Compile, doc) := Seq.empty,
  publishArtifact := false,
  publish := {}
)

lazy val publishableSettings = Def.settings(
  publishMavenStyle := true,
  licenses += "BSD" -> url("https://github.com/scalameta/scalameta/blob/master/LICENSE.md"),
  pomExtra := (
    <url>https://github.com/scalameta/scalameta</url>
    <inceptionYear>2017</inceptionYear>
    <scm>
      <url>git://github.com/scalameta/scalameta.git</url>
      <connection>scm:git:git://github.com/scalameta/scalameta.git</connection>
    </scm>
    <issueManagement>
      <system>GitHub</system>
      <url>https://github.com/scalameta/scalameta/issues</url>
    </issueManagement>
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com/</url>
      </developer>
    </developers>
  )
)

lazy val isFullCrossVersion = Seq(
  crossVersion := CrossVersion.full,
  unmanagedSourceDirectories.in(Compile) += {
    // NOTE: sbt 0.13.8 provides cross-version support for Scala sources
    // (http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#Cross-version+support+for+Scala+sources).
    // Unfortunately, it only includes directories like "scala_2.11" or "scala_2.12",
    // not "scala_2.11.8" or "scala_2.12.1" that we need.
    // That's why we have to work around here.
    val base = sourceDirectory.in(Compile).value
    base / ("scala-" + scalaVersion.value)
  }
)
