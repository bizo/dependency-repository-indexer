package depsearch.db

import depsearch.common.model._

trait DependencyDB {
  def update(d: Dependency): Unit
  def search(query: String, limit: Int): Seq[DependencyGroup]
  def dependency(org: String, group: String, numVersions: Int): Option[DependencyResult]
  def downstream(org: String, group: String): Seq[DependencyGroup]
}