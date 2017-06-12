package scala.meta.internal.sbthost

sealed trait Symbol {
  def syntax: String
  def structure: String
}

object Symbol {
  private def unsupported(sym: Symbol, op: String) = {
    val receiver = if (sym == None) "Symbol.None" else sym.syntax
    sys.error(s"Symbol.${op} not supported for $receiver")
  }

  case object None extends Symbol {
    override def toString = syntax
    // + deviation from scalameta
    override def syntax = "_root_."
    // override def syntax = ""
    // - deviation from scalameta
    override def structure = s"""Symbol.None"""
  }

  case class Local(id: String) extends Symbol {
    override def toString = syntax
    override def syntax = id
    override def structure = s"""Symbol.Local("$id")"""
  }

  case class Global(owner: Symbol, signature: Signature) extends Symbol {
    override def toString = syntax
    override def syntax = s"${owner.syntax}${signature.syntax}"
    override def structure = s"""Symbol.Global(${owner.structure}, ${signature.structure})"""
  }

  case class Multi(symbols: Seq[Symbol]) extends Symbol {
    override def toString = syntax
    override def syntax = symbols.map(_.syntax).mkString(";")
    override def structure = s"""Symbol.Multi(${symbols.map(_.structure).mkString(", ")})"""
  }
}

sealed trait Signature {
  def name: String
  def syntax: String
  def structure: String
}

object Signature {
  case class Type(name: String) extends Signature {
    override def syntax = s"${encodeName(name)}#"
    override def structure = s"""Signature.Type("$name")"""
    override def toString = syntax
  }

  case class Term(name: String) extends Signature {
    override def syntax = s"${encodeName(name)}."
    override def structure = s"""Signature.Term("$name")"""
    override def toString = syntax
  }

  case class Method(name: String, jvmSignature: String) extends Signature {
    override def syntax = s"${encodeName(name)}$jvmSignature."
    override def structure = s"""Signature.Method("$name", "$jvmSignature")"""
    override def toString = syntax
  }

  case class TypeParameter(name: String) extends Signature {
    override def syntax = s"[${encodeName(name)}]"
    override def structure = s"""Signature.TypeParameter("$name")"""
    override def toString = syntax
  }

  case class TermParameter(name: String) extends Signature {
    override def syntax = s"(${encodeName(name)})"
    override def structure = s"""Signature.TermParameter("$name")"""
    override def toString = syntax
  }

  case class Self(name: String) extends Signature {
    override def syntax = s"${encodeName(name)}=>"
    override def structure = s"""Signature.Self("$name")"""
    override def toString = syntax
  }

  private def encodeName(name: String): String = {
    val headOk = Character.isJavaIdentifierStart(name.head)
    val tailOk = name.tail.forall(Character.isJavaIdentifierPart)
    if (headOk && tailOk) name else "`" + name + "`"
  }
}
