import org.apache.spark._
import java.math.BigInteger
import scala.util.control.Breaks._

object SparkDataPreprocess {

  def main(args: Array[String]) {
    if (args.length == 0) {
      System.err.println("Usage: SparkDataPreprocess <master> [<slices>]")
      System.exit(1)
    }

    def nat(beg: Int): Stream[Int] = beg #:: nat(beg + 1)

    val sc = new SparkContext(args(0), "SparkDataPreprocess",
      System.getenv("SPARK_HOME"), SparkContext.jarOfClass(this.getClass).toSeq)

    val lines = sc.textFile("hdfs://student3-x1:9000/movies.txt")

    val linesWithIndex = lines.zipWithIndex
//    linesWithIndex.persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_2)

    val splits = linesWithIndex.filter(lineIndex => lineIndex._1.isEmpty()).map(lineIndex => lineIndex._2)
    splits.persist()
    val interval = splits.first()

    def groupfunc(ele: (String, Long)): Long = {
      (ele._2 + 1) / (interval + 1).asInstanceOf[Long]
    }

    val groups = linesWithIndex.groupBy(groupfunc)

    def murmurHash(word: String, seed: Int): Int = {
      val c1 = 0xcc9e2d51
      val c2 = 0x1b873593
      val r1 = 15
      val r2 = 13
      val m = 5
      val n = 0xe6546b64
 
      var hash = seed
 
      for (ch <- word.toCharArray) {
        var k = ch.toInt
        k = k * c1
        k = (k << r1) | (hash >> (32 - r1))
        k = k * c2
 
        hash = hash ^ k
        hash = (hash << r2) | (hash >> (32 - r2))
        hash = hash * m + n
      }
 
      hash = hash ^ word.length
      hash = hash ^ (hash >> 16)
      hash = hash * 0x85ebca6b
      hash = hash ^ (hash >> 13)
      hash = hash * 0xc2b2ae35
      hash = hash ^ (hash >> 16)
 
      hash
    }

    def fieldValue(ele: Iterable[(String, Long)]): String = {
        ele.foldLeft("") { (str, x) => {
            if (x._1.contains("product/productId")) {
                if (x._1.split(":")(1).trim.contains("#oc-")) {
                  murmurHash(x._1.split(":")(1).trim.split("#oc-")(1), 16).toString()
                } else {
                 murmurHash(x._1.split(":")(1).trim, 16).toString()
                }
            } else if (x._1.contains("review/userId") && !(str.isEmpty())) {
                murmurHash(x._1.split(":")(1).trim, 16).toString() + "," + str
            } else if (x._1.contains("review/score") && !(str.isEmpty())) { 
                str + "," + x._1.split(":")(1).trim
            } else {
                str
            }
        }}
    }

    val stringGroups = groups.map(group => fieldValue(group._2))

    stringGroups.saveAsTextFile("/formatInputData")

  }
}

