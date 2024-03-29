package core

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.reflect.ClassTag

/**
 * @Author: leebin
 * @Date: 2019/12/27 11:55 下午
 * @Description:
 */
object WordCount {
  def main(args: Array[String]): Unit = {
    /*
    fixme: SparkConf源码解析
    SparkConf类很简单
    private val settings = new ConcurrentHashMap[String, String]()
    fixme: SparkContext源码解析
    sc是整个spark的入口程序，里面包含了众多方法。网上有很多对SparkContext的解析：https://www.cnblogs.com/xia520pi/p/8609602.html
     */
    val conf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("WordCount")
    val sc: SparkContext = new SparkContext(conf)

    //源码里面有这种声明方式
    val context = new SparkContext("", "", "", Nil, Map("" -> ""))

    //查看有哪些输入方式或者RDD的构造方式？
    val lines: RDD[String] = sc.textFile("in")
    val words: RDD[String] = lines.flatMap(_.split(" "))
    val word2one: RDD[(String, Int)] = words.map((_, 1))
    val word2sum: RDD[(String, Int)] = word2one.reduceByKey(_ + _)
    val result: Array[(String, Int)] = word2sum.collect()

    /*
    fixme: RDD生成源码解析
    RDD的创建都是走parallelism
    def makeRDD[T: ClassTag](seq: Seq[T], numSlices: Int = defaultParallelism): RDD[T]
    defaultParallelism隐藏的很深，需要如下技巧才能找到最开始的代码：
    1.光标的控制，进入(alt+B)和后退(opt+cmd+LR)
    2.代码行数,ctrl+G
    3.查看当前Structure(ctrl+f12)或者打开Structure面板
    4.查看接口的继承实现关系ctrl+H
     */
    val rdd1: RDD[Int] = sc.makeRDD(List(1, 2, 3))
    val rdd2: RDD[Int] = sc.makeRDD(Array(1, 2, 3, 4), 2)
    val rdd3: RDD[Int] = sc.parallelize(List(1, 2, 3))
    val rdd4: RDD[String] = sc.textFile("input")
    //删除到文件，可以看到默认分区为8个
    rdd1.saveAsTextFile("output")

    /*
    fixme: map源码解析
    移动计算不移动数据，将map转换成MapPartitions
    new MapPartitionsRDD[U, T](this, (context, pid, iter) => iter.map(cleanF))
    */
    val rdd5: RDD[Int] = sc.makeRDD(1 to 10).map(x => x * 2)
    val rdd6: RDD[Int] = sc.makeRDD(1 to 10).map(_ * 2)

    /*
    fixme: mapPartitions与map与mapPartitionsWithIndex的区别
    遍历分区 vs 遍历元素
    业务角度：map好用(发数据消耗资源) vs
    性能角度：mapPartitions好用(发送计算,减少发送到执行器的交互时间，但是消耗资源严重占用整个分区，可能OOM)
    //def mapPartitionsWithIndex[U: ClassTag](f: (Int, Iterator[T]) => Iterator[U], preservesPartitioning: Boolean = false): RDD[U]
    mapPartitionsWithIndex:遍历分区,显示分区号(重点)
    flatMap的用法: 遍历展开
     */
    val mapPartitionsRdd1: RDD[Int] = rdd1.map(data => data * 2)
    val mapPartitionsRdd2: RDD[Int] = rdd1.mapPartitions(data => data)
    val mapPartitionsRdd3: RDD[Int] = rdd1.mapPartitions(data => data.map(d => d * 2))
    //分区号
    val indexRdd: RDD[Int] = rdd1.mapPartitionsWithIndex((num, data) => data.map(d => d * num))
    val tupleRdd: RDD[(Int, String)] = rdd1.mapPartitionsWithIndex {
      case (num, data) => {
        println(num)
        //data
        data.map((_, ":分区号:" + num))
      }
    }
    val flatMapRdd1: RDD[List[Int]] = sc.makeRDD(Array(List(1, 2), List(3, 4)))
    val flatMapRdd2: RDD[Int] = flatMapRdd1.flatMap(data => data)

    //fixme: glom算子(一撇，偷)将每个分区想成一个数组形成新的RDD defaultParallelism和glom
    val glomRdd: RDD[Array[Int]] = sc.makeRDD(List(1, 2, 3, 4, 5), 3).glom()
    glomRdd.foreach(arr => {
      for (elem <- arr) {
        println(elem)
      }
    })
    println(result)

    //fixme:
  }
}
