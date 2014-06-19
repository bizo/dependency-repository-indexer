package depsearch.indexer.s3

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import scala.collection.JavaConverters._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import java.util.concurrent.TimeUnit
import com.amazonaws.services.s3.model.ListObjectsRequest
import S3Lister._
import depsearch.indexer.IvyDependencyParser
import depsearch.db.DependencyDB
import depsearch.common.model.Dependency
import depsearch.db.mongo.MongoDependencyDB
import java.io.InputStream

class S3Indexer(db: DependencyDB) {
  val s3: AmazonS3 = new AmazonS3Client
  val executor = Executors.newFixedThreadPool(5)
  
  def index(bucket: String, prefix: Option[String] = None) {
    val r = new ListObjectsRequest().withBucketName(bucket).withPrefix(prefix.getOrElse(null))
    
    s3.listBatch(r) { list =>
      list.grouped(100) foreach { g =>
        executor.submit(new IndexWorker(s3, db, g))
      }
    }
    
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.MINUTES)
  }
  
  class IndexWorker(s3: AmazonS3, db: DependencyDB, list: Iterable[S3ObjectSummary]) extends Callable[Boolean] {
    val parser = new IvyDependencyParser
    
    val ivyFilePattern = """.*/ivy-[^/]+.xml$""".r
    
    private def getObject(o: S3ObjectSummary): InputStream = {
      val obj = s3.getObject(new GetObjectRequest(o.getBucketName, o.getKey))
      return obj.getObjectContent
    }
    
    def call(): Boolean = {
      for (elem <- list) {
        if (ivyFilePattern.findFirstIn(elem.getKey()).isDefined) {
          val in = getObject(elem)
          
          try {
            db.update(parser.parse(in))
          } catch {
            case e: Exception => {
              System.err.println("+" * 50)
              System.err.println(elem.getKey() + ", " + e.getMessage())
              System.err.println("+" * 50)              
            }
          } finally {
            in.close()
          }
        }
      }

      true
    }
  }
}



object S3Indexer {
  def main(args: Array[String]) {
    val db = MongoDependencyDB()

    val bucket = args(0)
    val prefix = if (args.length > 1) Some(args(1)) else None

    new S3Indexer(db).index(bucket, prefix)
  }
}