package com.app

import com.typesafe.scalalogging.LazyLogging

import java.sql.{Connection, DriverManager}

object Application extends App with LazyLogging {
  // benchmark Variables
  val numBatches = 10
  val batchSize =
    5000 // this times the number of parameters (4) needs to be smaller than 32,767 (2 byte complement)
  //  postgres variables
  val driver = "org.postgresql.Driver"
  // reWriteBatchedInserts seems to only work for very simple Insert statements (e.g. not in sub queries)
  val url = "jdbc:postgresql://localhost:35432/main?reWriteBatchedInserts=true&connectTimeout=3"
  val username = "user"
  val password = "pass"
  var connection = None: Option[Connection]

  def runCase(name: String, testRun: (Connection, Int, Int) => Unit): Unit = {
    val cleanupStatement = connection.get.createStatement()
    cleanupStatement.execute("DELETE FROM readings;")
    cleanupStatement.execute("VACUUM FULL measure_point_dim, asset_dim, readings;")
    logger.info(s"started - $name: Inserting $numBatches batches of $batchSize readings")
    val startTime = System.nanoTime()
    testRun(connection.get, numBatches, batchSize)
    logger.info(s"finished - $name: ${(System.nanoTime() - startTime) / 1e9d}s")
  }

  Class.forName(driver) // this line causes some weird side effect of driver registration

  try {
    connection = Some(DriverManager.getConnection(url, username, password))
    connection.get.setAutoCommit(true)

    runCase("compacted", Compacted.run)
    runCase("naive", Naive.run)
    // runCase("stupid", Stupid.run) - takes for 10 batches of 5000 readings ~ 110s
  } catch {
    case e: Throwable => e.printStackTrace
  } finally {
    if (connection.isDefined) connection.get.close()
  }
}
