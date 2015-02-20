package depsearch.db

import depsearch.common.model._
import com.google.common.cache.CacheLoader
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import com.google.common.cache.Weigher
import com.google.common.cache.Cache

class CachedDependencyDB(db: DependencyDB) extends DependencyDB {
  private val downstreamCache = {
    val loader = new CacheLoader[DependencyGroup, Seq[DependencyGroup]]() {
      override def load(d: DependencyGroup): Seq[DependencyGroup] = db.downstream(d.org, d.group)
    }
    
    val weigher = new Weigher[DependencyGroup, Seq[DependencyGroup]]() {
     override def weigh(d: DependencyGroup, r: Seq[DependencyGroup]): Int = r.size
    }
    
    CacheBuilder.newBuilder().weigher(weigher).maximumWeight(200000).expireAfterWrite(5, TimeUnit.MINUTES).build(loader)
  }
  
  override def update(d: Dependency) {
    db.update(d)
  }
  
  override def search(query: String, limit: Int): Seq[DependencyGroup] = {
    db.search(query, limit)
  }
  
  override def dependency(org: String, group: String, numVersions: Int): Option[DependencyResult] = {
    db.dependency(org, group, numVersions)
  }
  
  override def downstream(org: String, group: String): Seq[DependencyGroup] = {
    downstreamCache.get(DependencyGroup(org, group))
  }
  
  override def stats(): IndexStats = db.stats
  override def setLastUpdated(when: java.util.Date) = db.setLastUpdated(when)
}