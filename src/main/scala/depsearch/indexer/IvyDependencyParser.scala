package depsearch.indexer

import java.io.InputStream
import scala.xml.XML
import depsearch.common.model._

class IvyDependencyParser extends DependencyParser {
  def parse(in: InputStream): Dependency = {
    val module = XML.load(in)
    
    val info = module \ "info"
    
    val version = Version(info \ "@revision" text, info \ "@branch" text, info \ "@status" text)
    
    val description = {
      val d = info \ "description"
      
      if (d.isEmpty) {
        None
      } else {
        val t = d.text
        val h = d \ "@homepage" text
        
        if (t.isEmpty && h.isEmpty) {
          None
        } else {
          Some(Description(d.text, d \ "@homepage" text))
        }
      }
    }
    
    val license = {
      val l = info \ "license"
      if (l.isEmpty) {
        None
      } else {
        Some(License(l \ "@name" text, l \ "@url" text))
      }
    }
    
    val org = info \ "@organisation" text
    val group = info \ "@module" text
    val publication = {
      val p = info \ "@publication"
      
      if (p.isEmpty) {
        None
      } else {
        Some((p text) toLong)
      }
    }
    
    val artifacts = module \ "publications" \ "artifact" map { a =>
      Artifact(a \ "@name" text, a \ "@type" text, a \ "@ext" text)
    }
    
    val deps = module \ "dependencies" \ "dependency" map { d =>
      val conf = {
        val c = d \ "@conf"
        
        if (c.isEmpty) None else Some((c text))
      }      
      
      ShortDependency(d \ "@org" text, d \ "@name" text, d \ "@rev" text, conf)
    }
    
    Dependency(org, group, version, publication, artifacts, description, license, deps)
  }
}