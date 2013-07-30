ScalaJDO
========

A project to use the DataNucleus JDO persistence library from Scala.

Currently, the project just defines a `DataStore` class that allows
you to fetch a `ThreadLocal` `PersistenceManager`. A
`ScalaPersistenceManager` is defined, which provides for creation
of typesafe queries that return Scala types.

Unfortunately, the best way to handle Scala collections at the moment
is to define them as private Java collections and then convert back
and forth to Scala collections as items are fetched from and
committed back to the database. One of the major goals for the project
is to provide plug-in support for Scala collections and other common
Scala types, but this requires some modification of the underlying
DataNucleus code.
