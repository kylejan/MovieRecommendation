import org.apache.spark._

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

    // val partitionsNum = lines.count() / lines.partitions.length
    val partitionsSize = lines.mapPartitions(iter => Array(iter.size).iterator, true).collect()

    val linesWithIndex = lines.mapPartitionsWithIndex { (pi, it) => {
      var i = {
        if (pi != 0) {
          var pastSize = 0.asInstanceOf[Long]
          var size = 0
          for (size <- 0 to (pi - 1))
            pastSize += partitionsSize(size).asInstanceOf[Long]
          pastSize
        } else 0.asInstanceOf[Long]
      }
      val newIt = it.map(line => {
        i += 1
        (line, i)
      })
      newIt
    }}

/*
    linesWithIndex.collect().foreach{ x => {
      println(x._1 + " " + x._2)
    }}
*/

    val splitIndexes = linesWithIndex.filter(lineIndex => lineIndex._1.isEmpty()).map(lineIndex => lineIndex._2)

    //splitIndexes.collect().foreach{ x => println(x) }

    val contentLines = linesWithIndex.filter(lineIndex => !(lineIndex._1.isEmpty()))

    val interval = splitIndexes.first()
    
    def groupfunc(ele: (String, Long)): Long = {
      ele._2 / interval
    }

    val groups = contentLines.groupBy(groupfunc)

/*
    groups.collect().foreach{ x => {
      x._2.foreach{ y => println(y._1) } 
      }
      println("------------------")
    }
*/

    def fieldValue(ele: Iterable[(String, Long)]): String = {
      var str = ""
      ele.foreach { x => {
        if (x._1.contains("product/productId") || x._1.contains("review/score")) {
          if (str.isEmpty()) { str = x._1.split(":")(1).trim }
          else { str = str + "," + x._1.split(":")(1).trim }
        } else if (x._1.contains("review/userId")) {
          str = x._1.split(":")(1).trim + "," + str
        }
      }}
      str
    }

    val stringGroups = groups.map(group => fieldValue(group._2))

    stringGroups.saveAsTextFile("formatInputData")
    //stringGroups.collect().foreach {x => println(x + "\n") }
  }
}

