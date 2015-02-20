package depsearch.db.mongo

import depsearch.db.DependencyDB
import depsearch.common.model._
import com.mongodb.DB
import com.mongodb.BasicDBObject
import com.mongodb.BasicDBList
import scala.collection.JavaConverters._
import com.mongodb.MongoException
import com.mongodb.Mongo
import com.mongodb.MongoClient
import com.mongodb.DBObject
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import com.mongodb.WriteConcern

class MongoDependencyDB(db: DB) extends DependencyDB {
  val versionParser = new VersionParser()
  
  override def search(query: String, limit: Int): Seq[DependencyGroup] = {
    val group = db.getCollection("repo_v2_group")
    
    val cur = group.find(new BasicDBObject("tags", query))
    
    val res = cur.limit(limit).toArray().asScala
    
    res.map { r =>
      DependencyGroup(r.get("org").toString, r.get("group").toString)
    }
  }
  
  override def dependency(org: String, group: String, numVersions: Int): Option[DependencyResult] = {
    val q = new BasicDBObject
    q.put("org", org)
    q.put("group", group)
    
    val cur = db.getCollection("repo_v2").find(q)
    cur.sort(new BasicDBObject("versionSort", -1))
    
    val results = cur.limit(numVersions).toArray().asScala
    
    if (results.isEmpty) {
      return None
    }
    
    val latest = toDependency(results(0))
    
    val versions = results.map { d =>
      toDependency(d).version
    }
   
    Some(DependencyResult(latest, versions))
  }
  
  override def downstream(org: String, group: String): Seq[DependencyGroup] = {
    val q = new BasicDBObject
    
    val d = new BasicDBObject()
    d.put("org", org)
    d.put("group", group)
    
    val e = new BasicDBObject()
    e.put("$elemMatch", d)
    
    q.put("dependencies", e)
    
    val cur = db.getCollection("repo_v2").find(q)
    
    val ret = new HashSet[DependencyGroup]
    
    for (dep <- cur.toArray().asScala.map(toDependency)) {
      ret.add(new DependencyGroup(dep.org, dep.group))
    }
    
    ret.toVector.sortBy { d => d.org + ":" + d.group }
  }
  
  override def stats(): IndexStats = {
    
    val repo = db.getCollection("repo_v2")
    
    val numDeps = repo.count()
    val numOrgs = repo.distinct("org").size
    
    val lastUpdated = {
      val cur = db.getCollection("repo_v2_meta").find()
      if (cur.hasNext()) {
        val d = cur.next().get("last_updated").asInstanceOf[Long]
        Some(new java.util.Date(d))
      } else {
        None
      }
    }
    
    new IndexStats(
      repo.count(),
      repo.distinct("org").size,
      repo.distinct("group").size,
      lastUpdated
    )
  }
  
  override def setLastUpdated(when: java.util.Date) {
    val col = db.getCollection("repo_v2_meta")
    
    val o = new BasicDBObject
    o.put("_id", 0)
    o.put("last_updated", when.getTime)
    
    val q = new BasicDBObject
    o.put("_id", 0)
    
    col.update(q, o, true, false, WriteConcern.NORMAL) 
  }


  private def toDependency(d: DBObject): Dependency = {
    Dependency(
      d.get("org").toString,
      d.get("group").toString,
      toVersion(dbo(d.get("version"))),
      toPublication(d.get("publication")),
      toArtifacts(dbo(d.get("artifacts"))),
      toDescription(dbo(d.get("description"))),
      toLicense(dbo(d.get("license"))),
      toShortDependencies(dbo(d.get("dependencies"))))
  }
  
  private def toDescription(d: DBObject): Option[Description] = {
    if (d != null) {
      Some(Description(d.get("text").toString, d.get("homepage").toString))
    } else {
      None
    }
  }
  
  private def toLicense(l: DBObject): Option[License] = {
    if (l != null) {
      Some(License(l.get("name").toString, l.get("url").toString))
    } else {
      None
    }
  }  
  
  private def toPublication(o: Object): Option[Long] = {
     if (o != null) {
      Some(o.asInstanceOf[Long])
    } else {
      None
    }
  }
    
  private def toArtifacts(artifacts: DBObject): Seq[Artifact] = {
     asListOfObjects(artifacts).map { o =>
       Artifact(o.get("name").toString, o.get("type").toString, o.get("ext").toString)
     }
  }
  
  private def toShortDependencies(deps: DBObject): Seq[ShortDependency] = {
     asListOfObjects(deps).map { toShortDependency }
  }  
  
  private def toShortDependency(o: DBObject): ShortDependency = {
    val c = Option(o.get("conf")).map(_.toString)
    ShortDependency(o.get("org").toString, o.get("group").toString, o.get("rev").toString, c)
  }
  
  private def asListOfObjects(d: DBObject): Seq[DBObject] = {
    d.toMap().values().asScala.map(_.asInstanceOf[DBObject]).toSeq
  }
  
  private def dbo(o: Object): DBObject = o.asInstanceOf[DBObject]
  
  private def toVersion(v: DBObject): Version = {
    Version(
        v.get("version").toString,
        v.get("branch").toString,
        v.get("status").toString)
  }
  
  override def update(d: Dependency): Unit = {
    val repo = db.getCollection("repo_v2")
    
    val o = new BasicDBObject
    o.put("org", d.org)
    o.put("group", d.group)
    
    for (pub <- d.publication) {
      o.put("publication", pub)
    }

    val version = {
      val v = new BasicDBObject
      v.put("version", d.version.version)
      v.put("branch", d.version.branch)
      v.put("status", d.version.status)
      v
    }
    
    o.put("version", version)
    o.put("versionSort", versionParser.sortVersion(d.version.version))
    
    val license = d.license.map { l =>
      val o = new BasicDBObject
      
      o.put("name", l.name)
      o.put("url", l.url)
      
      o
    }
    
    val description = d.description.map { d =>
      val o = new BasicDBObject
      
      o.put("text", d.text)
      o.put("homepage", d.homepage)
      
      o
    }
    
    if (license.isDefined) {
      o.put("license", license.get)
    }
    
    if (description.isDefined) {
      o.put("description", description.get)
    }
    
    val artifacts = d.artifacts.map { a => 
      val o = new BasicDBObject
      
      o.put("name", a.name)
      o.put("type", a.aType)
      o.put("ext", a.ext)
            
      o
    }
    
    val dependencies = d.dependencies.map { d =>
      val o = new BasicDBObject
      
      o.put("org", d.org)      
      o.put("group", d.group)
      o.put("rev", d.rev)
      
      for (c <- d.conf) { o.put("conf", c) }
      
      o
    }
    
    o.put("artifacts", artifacts.asJava)
    o.put("dependencies", dependencies.asJava)
    
    val tags = tokenize(d.org, d.group)
    tags.addAll(tokenize(d.artifacts.map(_.name).toSet.toSeq:_*))
    
    o.put("tags", tags)
    
    o.put("_id", d.id.toString)
    
    repo.update(new BasicDBObject("_id", d.id.toString), o, true, false)
    
    
    val group = db.getCollection("repo_v2_group")
    val g = new BasicDBObject
    val groupKey = s"${d.org}/${d.group}"
    g.put("_id", groupKey)
    g.put("org", d.org)
    g.put("group", d.group)
    g.put("tags", tags)
    
    group.update(new BasicDBObject("_id", groupKey), g, true, false)
  }
  
  
  private def tokenize(words: String*): java.util.Set[String] = {
    val s = new java.util.HashSet[String]
    
    for (t <- Seq('-', '.', '_')) {
      for (w <- words) {
        s.add(w)
        for (p <- w.split(t)) {
          s.add(p)
        }
      }
    }
    
    s
  }
}

object MongoDependencyDB {
  def apply(): MongoDependencyDB = {
    val host = Option(System.getenv("DB_PORT_27017_TCP_ADDR")).getOrElse("127.0.0.1")
    val db = new MongoClient(host, 27017).getDB("repo")
    
    new MongoDependencyDB(db)
  }
}