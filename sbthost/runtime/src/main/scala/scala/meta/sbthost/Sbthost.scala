package scala.meta
package sbthost

import scala.collection.mutable
import scala.meta.tokens.Token.Ident

object Sbthost {
  def patchMirror(mirror: Mirror): Database = {
    val entries = mirror.database.entries.map {
      case (input, attrs) =>
        val endByStart = input.tokenize.get.collect {
          case t: Ident => t.pos.start -> t.pos.end
        }.toMap
        val isTaken = mutable.Set.empty[Point]
        val names = attrs.names.collect {
          case (pos @ Position.Range(_, start, end), symbol)
              if !isTaken(start) && endByStart.contains(start) =>
            isTaken += start
            (pos.copy(end = endByStart(start)), symbol)
        }
        input -> attrs.copy(names = names)
    }
    Database(entries)
  }
}
