# GRPCRDDExample
Demonstration of an RDD built from a gRPC stream. Spark SQL queries can be performed on the data, once the appropriate tabular structure has been given to the RDD.

In this example, a custom Spark RDD ([MetadataStoreRDD](https://github.com/wroughtiron/GRPCRDDExample/blob/master/src/main/scala/example/MetadataStoreRDD.scala)) is built from access log data stored on a local server ([MetadataStoreServer](https://github.com/wroughtiron/GRPCRDDExample/blob/master/src/main/java/example/MetadataStoreServer.java)) in the form of Result objects. The server is connected to a client ([MetadataStoreClient](https://github.com/wroughtiron/GRPCRDDExample/blob/master/src/main/java/example/MetadataStoreClient.java
)) on the RDD side via a gRPC connection. The seamless transition of data from client to RDD allows for the RDD to be constructed from the server data with ease. Tabular structure is given to the RDD by building a Spark DataFrame from it. Different geoIP queries can be performed using Spark SQL.

## Spark DataFrame and SparkSQL

Multiple DataFrames can be constructed and queried using Spark SQLContext.

In this example, the following DataFrames were constructed:

1. ResultsDF tabulates the appropriate access log data

2. CityBlocksDF tabulates data that maps CIDR IPv4 Blocks to "geoname ID's"

3. CityLocationDF tabulates data that maps "geoname ID's" to various English geographical information

The original csv files from which the latter two DataFrames were constructed can be found in this zip: [GeoLite2 City] (http://geolite.maxmind.com/download/geoip/database/GeoLite2-City-CSV.zip)

First a DataFrame (resultAndLocationsDF) is built mapping IP adresses of the access logs to CIDR IPv4 blocks, CIDR IPv4 blocks to geoname ID's and geoname_ID's to geographical information (including city names.) Then another DataFrame (cityNumbersDF) is built from a count query that is performed, counting the number of hits stored in the RDD for each city successfully mapped. This DataFrame is written to a plain text csv file. The following plot was constructed using this data:

![alt tag](https://raw.githubusercontent.com/wroughtiron/GRPCRDDExample/master/Number_of_Hits_Per_City.png)

For more information on DataFrames and Spark SQL please visit: [Spark SQL and DataFrame Guide] (https://spark.apache.org/docs/latest/sql-programming-guide.html)

## Purpose of the RDD

With lazy implementation, usage of the RDD is scalable in terms of size. With scala, functional programming is also well supported by the RDD with the map() and flatmap() functions.

In this example, data in all RDD's are manipulated with ease. An example of such manipulation is the simple calculation of the numeric maximum and minimum of each CIDR IPv4 Block in CityBlocksRDD. It must be noted that the RDD of Results will try to connect to the server every time it must be recomputed, unless persist() is called.

For more information on the Spark RDD, please visit: [Spark Programming Guide] (http://spark.apache.org/docs/latest/programming-guide.html)

## RDD Computation

Central to the implementation of the RDD is its partitioning. In this example, this was done on 128-bit UUID's, stored as fields in the Result objects. Each UUID is computed by hashing the entire access log line it is paired with.

Computation of data in each partition is done by means of an iterator for that partition. In this example, MetadataStoreRDD is constructed with the necessary information to form a gRPC connection and initializes an instance of MetadataStoreClient. The client is called to make a gRPC call for every partition computation and the stream returned is cast directly to the necessary iterator.

## Client - Server Connection

By nature of gRPC, only client and server implementation are required by the programmer. [Stubs] (https://github.com/wroughtiron/GRPCRDDExample/blob/master/build/generated-sources/main/anything/protocode/MetadataStoreGrpc.java) and [interfaces] (https://github.com/wroughtiron/GRPCRDDExample/blob/master/build/generated-sources/main/anything/protocode/Interface.java) are generated, using that which is specified in [interface.proto](https://github.com/wroughtiron/GRPCRDDExample/blob/master/src/main/proto/interface.proto).

In this example, a server-side streaming gRPC is established. The client sends a single Criteria message to the server, which returns a stream of Result messages.

For more information on different gRPC implementations, please visit on of the tutorials in the following link [gRPC Documentation] (http://www.grpc.io/docs/)

## Data Serialization

In a gRPC connection, data serialization is made possible by protocol buffers. The fields, field data types and other information about messages in the gRPC interface are specified in the service ([interface.proto](https://github.com/wroughtiron/GRPCRDDExample/blob/master/src/main/proto/interface.proto))

In this example, the following messages were sent:

1. Criteria messages containing the fields:
  - minID - The minimum UUID of the calling partition computation
  - maxID - The maximum UUID of the calling partition computation
  - keyRegex - The regular expression to which all key fields of Results in the RDD must match
  
2. Result messages containing the fields:
  - ID - The UUID of that access log
  - Time - The time that the user request was processed by the server that produced the access log
  - Key - The request line of the user
  - Value - The IPv4 address of the user

Quering of whether each Result object's ID was in the UUID range and whether each Key matched the keyRegex was done in [MetadataStoreIMPL](https://github.com/wroughtiron/GRPCRDDExample/blob/master/src/main/java/example/MetadataStoreIMPL.java).

For more information on message specification, please visit: [Protocol Buffers] (https://developers.google.com/protocol-buffers/docs/overview)

## Server Data

In this example, data was parsed from a text file in Common Log Format. For more information on different log formats, please visit [Log Files] (http://httpd.apache.org/docs/1.3/logs.html)

It must be noted that only numeric data IP addresses can be queried on CIDR Blocks. The original text file (with hostnames specified instead of IP addresses in places) can be found at: [EPA-HTTP] (http://ita.ee.lbl.gov/html/contrib/EPA-HTTP.html)
