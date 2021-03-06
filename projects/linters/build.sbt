organization  := "im.tox"
name          := "linters"
scalaVersion  := "2.11.7"

// Build dependencies.
libraryDependencies ++= Seq(
  "org.brianmckenna" %% "wartremover" % "0.14"
)

// Test dependencies.
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0-M14"
) map (_ % Test)

import im.tox.sbt.lint.Scalastyle
Scalastyle.projectSettings
