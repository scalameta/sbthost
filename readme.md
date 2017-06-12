# sbthost - Semantic API for sbt 0.13

[![Build Status](https://travis-ci.org/scalameta/sbthost.svg?branch=master)](https://travis-ci.org/scalameta/sbthost)
[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/scalameta)

The objective of this project is aid migration from sbt v0.13 to v1.0.
In particular, the objective is to allow
[sbt-migration-rewrites](https://github.com/scalacenter/sbt-migration-rewrites)
to use the Scalameta [Semantic API](http://scalameta.org/tutorial/#SemanticAPI)
for accurate source rewriting of sbt build files.
This project is not intended to be used for other purposes.
To do semantic analysis of sbt v1.0 builds, see
[Scalahost](http://scalameta.org/tutorial/#Scalahost).

This project consists of two modules:

- sbthost-nsc: a compiler plugin for Scala 2.10 that emits `.semanticdb` files
  (see [Semantic DB](http://scalameta.org/tutorial/#SemanticDB)) during compilation.
  sbthost-nsc only emits offset positions and it does not make an effort to
  disambiguate between resolved names in conflicting positions.
- sbthost-runtime: a 2.11/2.12 library that modifies/patches `.semanticdb` files,
  produced by sbthost-nsc. sbthost-runtime is used by applications like
  scalafix at *runtime*, as opposed to compile-time during creation of
  `.semanticdb` files.  sbthost-runtime does a best-effort of converting the
  emitted offset positions into range positions at "runtime" as well as to
  disambiguate between names at conflicting positions.



