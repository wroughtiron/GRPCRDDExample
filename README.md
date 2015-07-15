# GRPCRDDExample
Demonstration of an RDD built from data sent over a RPC stream from a remote or local server. SQL queries can be performed on the data, once the appropriate structure has been given to the RDD.

In this example, a custom Spark RDD (MetadataStoreRDD) is built from access log data stored on a local server (MetadataStoreServer) in the form of Result objects. The server is connected to a client (MetadataStoreClient) on the RDD side via a gRPC connection. The seamless transition of data from client to RDD allows for the RDD to be constructed from the server data with ease. Tabular structure is given to the RDD by building a Spark DataFrame from it. Different geoIP queries can be performed using Spark SQL.

## Spark DataFrame and SQL

Multiple DataFrames can be constructed and queried using Spark SQLContext.

In this example, the following DataFrames were constructed:

1. ResultsDF tabulates the appropriate access log data

2. CityBlocksDF tabulates data that maps CIDR IPv4 Blocks to "geoname ID's"

3. CityLocationDF tabulates data that maps "geoname ID's" to various English geographical information

The original csv files from which the latter two DataFrames were constructed can be found in this zip: [GeoLite2 City] (http://geolite.maxmind.com/download/geoip/database/GeoLite2-City-CSV.zip)

For more information on DataFrames and Spark SQL please visit: [Spark SQL and DataFrame Guide] (https://spark.apache.org/docs/latest/sql-programming-guide.html)

## Purpose of the RDD

With lazy implementation, usage of the RDD is scalable in terms of size. With scala, functional programming is also well supported by the RDD with the map() and flatmap() functions.

In this example, data in all RDD's are manipulated with ease. An example of such manipulation is the simple calculation of the numeric maximum and minimum of each CIDR IPv4 Block in CityBlocksRDD. It must be noted that the RDD of Results will try to connect to the server every time it must be recomputed, unless persist() is called.

For more information on the Spark RDD, please visit: [Spark Programming Guide] (http://spark.apache.org/docs/latest/programming-guide.html)

## RDD Computation

Central to the implementation of the RDD is its partitioning. In this example, this was done on 128-bit UUID's, stored as fields in the Result objects. Each UUID is computed by hashing the entire access log it is paired with.

Computation of data in each partition is done by means of an iterator for that partition. In this example, MetadataStoreRDD is constructed with the necessary information to form a gRPC connection and initializes an instance of MetadataStoreClient. The client is called to make a gRPC for every partition computation and the stream returned is casted directly to the necessary iterator.

## Client - Server Connection

By nature of the RPC connection, only client and server implementation are required by the programmer. All stubs and interfaces are generated, using that which is specified in the service.

In this example, a server-side streaming RPC is established. The client sends a single Criteria message to the server, which returns a stream of Result messages.

For more information on different gRPC implementations, please visit on of the tutorials in the following link [gRPC Documentation] (http://www.grpc.io/docs/)

## Data Serialization

In a gRPC connection, data serialization is made possible by data being grouped in a format specified by a message. The fields, field data types and other information about messages in the RPC are specified in the service (interface.proto)

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

For more information on servic specification, please visit: [Protocol Buffers] (https://developers.google.com/protocol-buffers/docs/overview)

## Server Data

In this example, data was parsed from a text file in Common Log Format. For more information on different log formats, please visit [Log Files] (http://httpd.apache.org/docs/1.3/logs.html)

It must be noted that only numeric data IP addresses can be queried on CIDR Blocks. The original text file (with hostnames specified instead of IP addresses in places) can be found at: [EPA-HTTP] (http://ita.ee.lbl.gov/html/contrib/EPA-HTTP.html)
