package depsearch.indexer

import org.junit.Test
import org.junit.Assert._
import depsearch.common.model.Version
import depsearch.common.model.License
import depsearch.common.model.Dependency
import java.util.Calendar

class IvyDependencyParserTest {
  @Test
  def test() {
    val dep = parseDep("depsearch/indexer/test-ivy-files/ivy-1.6.2.xml")
    
    assertEquals(("com.amazonaws", "aws-java-sdk"), (dep.org, dep.group))
    assertEquals(Version("1.6.2", "default", "release"), dep.version)
    assertEquals(License("Apache License, Version 2.0", "https://aws.amazon.com/apache2.0"), dep.license.get)
    
    val pubDate = {
      val c = Calendar.getInstance
      c.set(Calendar.YEAR, 2013)
      c.set(Calendar.MONTH, 9) // month start at 0?
      c.set(Calendar.DAY_OF_MONTH, 17)
      c.set(Calendar.HOUR_OF_DAY, 17)
      c.set(Calendar.MINUTE, 5)
      c.set(Calendar.SECOND, 39)
      c.set(Calendar.MILLISECOND, 0)
      c.getTime
    }
    
    assertEquals(pubDate, dep.publication.get)
    
    println(dep.dependencies)
  }
  
  private def parseDep(path: String): Dependency = {
    val s = getClass().getClassLoader().getResourceAsStream(path)
    
    val dep = new IvyDependencyParser().parse(s)
    s.close()
    
    dep
  }
}