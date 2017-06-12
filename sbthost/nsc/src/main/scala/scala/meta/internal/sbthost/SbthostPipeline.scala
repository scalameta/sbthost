package scala.meta.internal.sbthost

import java.nio.file.Files
import java.nio.file.Paths
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.meta.internal.semantic.schema
import scala.reflect.internal.util.OffsetPosition
import scala.reflect.internal.util.RangePosition
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.Phase
import scala.tools.nsc.io.VirtualFile
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.reporters.StoreReporter
import scala.meta.internal.semantic.{schema => s}

trait SbthostPipeline extends DatabaseOps { self: SbthostPlugin =>
  object SbthostComponent extends PluginComponent {
    val global: SbthostPipeline.this.global.type = SbthostPipeline.this.global
    override val runsAfter = List("typer")
    override val runsRightAfter = Some("typer")
    val phaseName = "sbthost"
    def getMessages(source: SourceFile): mutable.LinkedHashSet[s.Message] =
      g.reporter match {
        case reporter: StoreReporter =>
          reporter.infos.withFilter(_.pos.source == source).map { info =>
            val range = Option(info.pos).collect {
              case p: RangePosition => s.Range(p.start, p.end)
              case p: OffsetPosition => s.Range(p.point, p.point)
            }
            val severity = info.severity.id match {
              case 0 => s.Message.Severity.INFO
              case 1 => s.Message.Severity.WARNING
              case 2 => s.Message.Severity.ERROR
              case els => s.Message.Severity.UNKNOWN
            }
            s.Message(range, severity, info.msg)
          }
        case els =>
          //          global.reporter.warning(NoPosition, s"Unknown reporter $els")
          mutable.LinkedHashSet.empty
      }
    def getNames(unit: g.CompilationUnit): Seq[s.ResolvedName] = {
      val buffer = ListBuffer.newBuilder[s.ResolvedName]
      object traverser extends g.Traverser {

        def isValidSymbol(symbol: g.Symbol) =
          symbol.ne(null) && symbol != g.NoSymbol

        override def traverse(tree: g.Tree): Unit = {
          if (tree.pos.isDefined &&
              tree.hasSymbol &&
              isValidSymbol(tree.symbol) &&
              isValidSymbol(tree.symbol.owner)) {
            val symbol = tree.symbol.toSemantic
            val range = s.Range(tree.pos.point, tree.pos.point)
            buffer += s.ResolvedName(Some(range), symbol.syntax)
          }
          super.traverse(tree)
        }
      }
      traverser(unit.body)
      buffer.result()
    }
    override def newPhase(prev: Phase) = new StdPhase(prev) {
      def apply(unit: g.CompilationUnit): Unit = {
        val sourcePath = unit.source.file match {
          case f: VirtualFile =>
            Paths.get(f.path)
          case els =>
            Paths.get(els.file.getAbsoluteFile.toURI)
        }
        val filename = config.relativePath(sourcePath)
        val attributes = s.Attributes(
          filename = filename.toString,
          contents = unit.source.content.mkString,
          dialect = "Scala210", // TODO: not hardcode
          names = getNames(unit),
          messages = getMessages(unit.source).toSeq,
          denotations = Nil,
          sugars = Nil
        )
        val semanticdbOutFile = config.semanticdbPath(filename)
        semanticdbOutFile.toFile.getParentFile.mkdirs()
        Files.write(semanticdbOutFile.normalize(), attributes.toByteArray)
      }
    }
  }
}
