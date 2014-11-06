package depsearch.common.model

case class Dependency(
  org: String,
  group: String,
  version: Version,
  publication: Option[Long],
  artifacts: Seq[Artifact],
  description: Option[Description],
  license: Option[License],
  dependencies: Seq[ShortDependency]
) {
  val id: DependencyId = DependencyId(org, group, version)
}

case class ShortDependency(
  org: String,
  group: String,
  rev: String,
  conf: Option[String]
)

case class DependencyResult(
  dependency: Dependency,
  versions: Seq[Version]
)  

case class DependencyGroup(
  org: String,
  group: String
)

case class DependencyId(org: String, group: String, version: Version) {
  override def toString() = {
    Seq(org, group, version.branch, version.status, version.version).mkString("/")
  }
}

case class Artifact(
  name: String,
  aType: String,
  ext: String
)  

case class Version(
  version: String,
  branch: String,  
  status: String
)  

case class License(
  name: String,
  url: String
)

case class Description(
  text: String,
  homepage: String
)