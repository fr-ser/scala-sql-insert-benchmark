package com.app

import java.sql.Connection
import scala.util.Random

object Compacted {
  def getInsertSQL(batchSize: Int) = {
    val parameterList = ("(?, ?, ?, ?)," * batchSize).dropRight(1)

    s"""
      |WITH raw_data (asset_id, measure_point_id, the_timestamp, the_value) AS ( VALUES $parameterList)
      |, insert_unknown_measure_points AS (
      |    INSERT INTO measure_point_dim (measure_point_id, name)
      |    SELECT measure_point_id, '__inferred' || measure_point_id
      |    FROM (SELECT DISTINCT measure_point_id FROM raw_data) as mp_ids
      |    WHERE NOT EXISTS (
      |        SELECT 1 FROM measure_point_dim as mp_dim WHERE mp_dim.measure_point_id = mp_ids.measure_point_id
      |    )
      |    RETURNING measure_point_sk, measure_point_id
      |)
      |, insert_unknown_assets AS (
      |    INSERT INTO asset_dim (asset_id, name)
      |    SELECT asset_id, '__inferred' || asset_id
      |    FROM (SELECT DISTINCT asset_id FROM raw_data) as a_ids
      |    WHERE NOT EXISTS (SELECT 1 FROM asset_dim as a_dim WHERE a_dim.asset_id = a_ids.asset_id)
      |    RETURNING asset_sk, asset_id
      |)
      |, joined_data AS (
      |    SELECT
      |        COALESCE(uk_a.asset_sk, a_dim.asset_sk) AS final_asset_sk
      |        , COALESCE(uk_mp.measure_point_sk, mp_dim.measure_point_sk) AS final_mp_sk
      |        , the_timestamp
      |        , the_value
      |    FROM raw_data
      |    LEFT JOIN asset_dim AS a_dim ON raw_data.asset_id = a_dim.asset_id
      |    LEFT JOIN insert_unknown_assets AS uk_a ON raw_data.asset_id = uk_a.asset_id
      |    LEFT JOIN measure_point_dim as mp_dim ON raw_data.measure_point_id = mp_dim.measure_point_id
      |    LEFT JOIN insert_unknown_measure_points as uk_mp ON raw_data.measure_point_id = uk_mp.measure_point_id
      |)
      |INSERT INTO readings (asset_sk, measure_point_sk, timestamp, value)
      |SELECT * FROM joined_data
      |ON CONFLICT (measure_point_sk, asset_sk, timestamp) DO UPDATE
      |SET value = EXCLUDED.value
      |""".stripMargin
  }

  def run(connection: Connection, numBatches: Int, batchSize: Int): Unit = {
    val preparedStatement = connection.prepareStatement(getInsertSQL(batchSize))
    for (_ <- 1 to numBatches) {
      for (index <- 0 until batchSize) {
        preparedStatement.setInt((index * 4) + 1, Random.between(1, 120))
        preparedStatement.setInt((index * 4) + 2, Random.between(1, 120))
        preparedStatement.setLong((index * 4) + 3, System.nanoTime())
        preparedStatement.setDouble((index * 4) + 4, Random.nextDouble())
      }
      preparedStatement.execute()
    }

  }
}
