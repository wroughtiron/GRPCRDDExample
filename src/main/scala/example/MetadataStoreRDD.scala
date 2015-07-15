
//  Copyright 2015 Roy Ferguson
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.


package example

import java.lang.Long
import java.math.BigInteger

import anything.protocode.Interface.Result
import com.google.protobuf.ByteString
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.{Partition, SparkConf, SparkContext, TaskContext}

import scala.collection.JavaConversions

/**
 * Created by roy on 2015/06/30.
 */

/*MetadataStoreRDD built from data stored on a remote server
* Remote server must be running with finished parsed data, before it can be accessed
* A grpc connection is established between an instance of MetadataStoreClient and the server
* port - host port number of client
* IP - hostname of client
* Results are partitioned based on magnitude of 128 bit ByteString uniformly distributed ID's
* minID - smallest ID that will be included in partitioning
* maxID - largest ID that will be included in partitioning
* only results whose Key field matches queryRegex will be returned*/

class MetadataStoreRDD[Result](sc: SparkContext, port: Int, IP: String, queryRegex: String) extends RDD[Any](sc, Nil) {
  val partitionArray = new Array[Partition](256)

  override def compute(split: Partition, context: TaskContext): Iterator[Result] = {
      val partition = split.asInstanceOf[MetadataStorePartition]
      val client = new MetadataStoreClient(IP, port)
      val results = JavaConversions.asScalaIterator(client.returnQuery(partition.lower, partition.upper, queryRegex)).asInstanceOf[Iterator[Result]]
      return results
  }

  override def getPartitions: Array[Partition] = {
    var i = 0
    (0 until 255).map(i => {
      var bounds = getPartitionBounds(i)
      new MetadataStorePartition(i, bounds._1, bounds._2)
    }).padTo(256, getLastPartition()).toArray[Partition]
  }

  def getLastPartition() : MetadataStorePartition = {
    val bounds = getLastBounds()
    new MetadataStorePartition(255, bounds._1, bounds._2)
  }

  def getLastBounds(): (ByteString, ByteString) = {
    val lowerArray: Array[Byte] = new Array[Byte](16)
    val upperArray: Array[Byte] = new Array[Byte](16)
    lowerArray(0) = 255.toByte
    for (i <- 1 until 16) {
      lowerArray(i) = 0
    }
    for (i <- 0 until 16) {
      lowerArray(i) = 255.toByte
    }
    (ByteString.copyFrom(lowerArray),
      ByteString.copyFrom(upperArray))
  }

  def getPartitionBounds(index: Int): (ByteString, ByteString) = {
    val lowerArray: Array[Byte] = new Array[Byte](16)
    val upperArray: Array[Byte] = new Array[Byte](16)
    lowerArray(0) = index.toByte
    for (i <- 1 until 16) {
      lowerArray(i) = 0
    }
    upperArray(0) = (index + 1).toByte
    for (i <- 1 until 16) {
      lowerArray(i) = 0
    }
    (ByteString.copyFrom(lowerArray),
      ByteString.copyFrom(upperArray))
  }

  class MetadataStorePartition(idx: Int, lowerBound: ByteString, upperBound: ByteString) extends Partition {
    override def index: Int = idx
    def lower = lowerBound
    def upper = upperBound
  }

}

/*resultsProcessor queries a data set containing access logs to establish where a given user request has been called from
* This is acheived using three data sets:
* resultsDF - DataFrame that contains time of request, user request (Key) and IP address (Value) from all relevant access logs
* cityBlocks - DataFrame that (among other data) maps IP addresses to 'geoname_id'
* cityLocation - DataFrame that maps geoname_id to geographic information (city name, country name, RIR, etc.)
* resultsAndLocationsDF - DataFrame that maps relevant information to each access log using SQL query*/
object ResultsProcessor {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("resultsProcessor").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val mdsr = new MetadataStoreRDD[Result](sc, 12345, "localhost", ".*")
    
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    val resultDF = sqlContext.createDataFrame(mdsr.map(rowify), ResultStructTypeBuilder())
    resultDF.persist()

    val cityBlocksRDD = resultDF.sqlContext.sparkContext.textFile(args(0))
    val cityBlocksSchema = CityBlockStructTypeBuilder(cityBlocksRDD.first().split(","))
    val cityBlocksRowRDD = cityBlocksRDD.mapPartitionsWithIndex(headerRemover).map(_.split(",", 9)).map(p => Row(p(0), p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8), convertCIDRRangeStringToMinLong(p(0)), convertCIDRRangeStringToMaxLong(p(0))))
    val cityBlocksDF = sqlContext.createDataFrame(cityBlocksRowRDD, cityBlocksSchema)
    cityBlocksDF.persist()
    
    val cityLocationRDD = resultDF.sqlContext.sparkContext.textFile(args(1))
    val cityLocationSchema = CityLocationStructTypeBuilder(cityLocationRDD.first().split(","))
    val cityLocationRowRDD = cityLocationRDD.mapPartitionsWithIndex(headerRemover).map(_.split(",", 13)).map(p => Row(p(0), p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8), p(9), p(10), p(11), p(12)))
    val cityLocationDF = sqlContext.createDataFrame(cityLocationRowRDD, cityLocationSchema)
    cityLocationDF.persist()
    
    val resultsAndLocationsDF = mapIPToLocation(resultDF, cityBlocksDF, cityLocationDF)
    
    println(resultDF.show())
    println(resultDF.count())
    println(cityBlocksDF.show())
    println(cityBlocksDF.count())
    println(cityLocationDF.show())
    println(cityLocationDF.count())
    println(resultsAndLocationsDF.show())
    println(resultsAndLocationsDF.count())
  }

  def ResultStructTypeBuilder (): StructType = {
    val structArray = new Array[StructField](5)
    structArray{0} = new StructField("ID", BinaryType, true, Metadata.empty)
    structArray{1} = new StructField("Time", LongType, true, Metadata.empty)
    structArray{2} = new StructField("Key", StringType, true, Metadata.empty)
    structArray{3} = new StructField("Value", StringType, true, Metadata.empty)
    structArray{4} = new StructField("IP_Long", LongType, true, Metadata.empty)
    new StructType(structArray)
  }

  private def CityBlockStructTypeBuilder(fieldNames: Array[String]): StructType = {
    val structArray = new Array[StructField](11)
    structArray{0} = new StructField(fieldNames{0}, StringType, true, Metadata.empty)
    structArray{1} = new StructField(fieldNames{1}, StringType, true, Metadata.empty)
    structArray{2} = new StructField(fieldNames{2}, StringType, true, Metadata.empty)
    structArray{3} = new StructField(fieldNames{3}, StringType, true, Metadata.empty)
    structArray{4} = new StructField(fieldNames{4}, StringType, true, Metadata.empty)
    structArray{5} = new StructField(fieldNames{5}, StringType, true, Metadata.empty)
    structArray{6} = new StructField(fieldNames{6}, StringType, true, Metadata.empty)
    structArray{7} = new StructField(fieldNames{7}, StringType, true, Metadata.empty)
    structArray{8} = new StructField(fieldNames{8}, StringType, true, Metadata.empty)
    structArray{9} = new StructField("Network_IP_Min", LongType, true, Metadata.empty)
    structArray{10} = new StructField("Network_IP_Max", LongType, true, Metadata.empty)
    new StructType(structArray)
  }

  private def CityLocationStructTypeBuilder(fieldNames: Array[String]): StructType = {
    val structArray = new Array[StructField](13)
    structArray{0} = new StructField(fieldNames{0}, StringType, true, Metadata.empty)
    structArray{1} = new StructField(fieldNames{1}, StringType, true, Metadata.empty)
    structArray{2} = new StructField(fieldNames{2}, StringType, true, Metadata.empty)
    structArray{3} = new StructField(fieldNames{3}, StringType, true, Metadata.empty)
    structArray{4} = new StructField(fieldNames{4}, StringType, true, Metadata.empty)
    structArray{5} = new StructField(fieldNames{5}, StringType, true, Metadata.empty)
    structArray{6} = new StructField(fieldNames{6}, StringType, true, Metadata.empty)
    structArray{7} = new StructField(fieldNames{7}, StringType, true, Metadata.empty)
    structArray{8} = new StructField(fieldNames{8}, StringType, true, Metadata.empty)
    structArray{9} = new StructField(fieldNames{9}, StringType, true, Metadata.empty)
    structArray{10} = new StructField(fieldNames{10}, StringType, true, Metadata.empty)
    structArray{11} = new StructField(fieldNames{11}, StringType, true, Metadata.empty)
    structArray{12} = new StructField(fieldNames{12}, StringType, true, Metadata.empty)
    new StructType(structArray)
  }

  def rowify (res: Any): Row = {
    val result = res.asInstanceOf[Result]
    Row(result.getId.toByteArray, result.getTime, result.getKey, result.getValue, convertIPStringToLong(result.getValue))
  }

  private def headerRemover(i: Int, iterator: Iterator[String]): Iterator[String] = {
    if (i == 0) {
      iterator.drop(1)
    } else {
      iterator
    }
  }

  def convertIPStringToLong (IPString: String): Long = {
    val longs = IPString.split("\\.").map(s => s.toLong)
    longs{0}*16777216 + longs{1}*65536 + longs{2}*256 + longs{3}
  }

  def convertCIDRRangeStringToMaxLong (IPString: String): Long = {
    val addressAndPrefix = IPString.split("/")
    val longs = addressAndPrefix{0}.split("\\.").map(s => s.toLong)
    val prefix = addressAndPrefix{1}.toInt
    val longIP = longs{0}*16777216 + longs{1}*65536 + longs{2}*256 + longs{3}
    val truncation = Math.pow(2, 32 - prefix).toLong
    longIP + truncation - 1
  }

  def convertCIDRRangeStringToMinLong (IPString: String): Long = {
    val addressAndPrefix = IPString.split("/")
    val longs = addressAndPrefix{0}.split("\\.").map(s => s.toLong)
    val prefix = addressAndPrefix{1}.toInt
    longs{0}*16777216 + longs{1}*65536 + longs{2}*256 + longs{3}
  }

  def mapIPToLocation(resultDF: DataFrame, cityBlocksDF: DataFrame, cityLocationDF: DataFrame): DataFrame = {
    resultDF.registerTempTable("resultTable")
    cityBlocksDF.registerTempTable("cityBlocksTable")
    cityLocationDF.registerTempTable("cityLocationTable")
    resultDF.sqlContext.sql(
      " SELECT resultTable.Time, resultTable.Key, resultTable.Value, cityBlocksTable.network, cityBlocksTable.geoname_id, cityLocationTable.geoname_id, cityLocationTable.continent_name, cityLocationTable.city_name" +
        " FROM resultTable" +
        " JOIN cityBlocksTable" +
        " ON resultTable.IP_Long BETWEEN cityBlocksTable.Network_IP_Min AND cityBlocksTable.Network_IP_Max" +
        " JOIN cityLocationTable" +
        " ON cityBlocksTable.geoname_id = cityLocationTable.geoname_id")
  }
}