package depsearch.indexer

import org.junit.Test
import org.junit.Assert._
import depsearch.common.model.Version
import depsearch.common.model.License
import depsearch.common.model.Dependency

class IvyDependencyParserTest {
  @Test
  def test() {
    val dep = parseDep("depsearch/indexer/test-ivy-files/ivy-1.6.2.xml")
    
    assertEquals(("com.amazonaws", "aws-java-sdk"), (dep.org, dep.group))
    assertEquals(Version("1.6.2", "default", "release"), dep.version)
    assertEquals(License("Apache License, Version 2.0", "https://aws.amazon.com/apache2.0"), dep.license.get)
    
    println(dep.dependencies)
  }
  
  private def parseDep(path: String): Dependency = {
    val s = getClass().getClassLoader().getResourceAsStream(path)
    
    val dep = new IvyDependencyParser().parse(s)
    s.close()
    
    dep
  }
}