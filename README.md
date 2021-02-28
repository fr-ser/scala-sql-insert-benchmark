# Scala SQL Insert Benchmark

The goal of this repo is to play around with (complex) SQL inserts into a postgres database.

## Software requirements

- make (for the commands)
- sbt (to run/compile the code)
- docker (to start up the local postgres database)

## Running the benchmark

The whole benchmark can be run with `make run`.
This starts up the database, runs the queries and stops the database

## Benchmark context

This benchmark is used to compare approaches, so the absolute numbers will vary on each machine.
For the below observations a batch of 5,000 readings was inserted 10 times.

Inspired by a real word case the inserts try the following:

- Readings are received as a real time stream of evens with a measure_point and asset ID.

- The individual readings are insert into a dimensional reporting schema (star schema)
   - the star schema has surrogate keys, which need to be joined before the insert -> no "simple" insert possible
   - new dimensions are inserted "on the fly". In case an "unknown asset ID" arrives it is added to the dimension
   
- the insert is performed as an "upsert" (on conflict do update)

## Observations

The option `reWriteBatchedInserts` does not work for (a bit) more complex SQL queries.
Using sub queries or CTEs prevents the rewrite behavior. For the actual behavior see:
<https://vladmihalcea.com/postgresql-multi-row-insert-rewritebatchedinserts-property/>
   
1. **Stupid approach: 110s for 50k inserts**. Common sense applies: Using a network round trip per insert is extremely
   slow.
   
2. **Naive batch: ~7s for 50k inserts**. Using the standard JDBC batching (even without the rewrites) improves the
   speed greatly.

3. **Compacted batch: ~1.5s for 50k inserts.** Batching the inserts (as the `reWriteBatchedInserts` option should)
   improves performance again greatly.
   - the maximum number of parameters seems to be the twos complement of 2 bytes (32,767)
