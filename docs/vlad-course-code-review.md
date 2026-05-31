# Vlad Course Based Code Review

This review was written from a high-performance Hibernate/JPA perspective inspired by
Vlad Mihalcea's guidance: deterministic pagination, bounded persistence context size,
careful fetch strategy, and predictable batch writes.

## Scope

- `QuerydslPagingItemReader`
- `QuerydslNoOffsetPagingItemReader`
- `QuerydslNoOffsetOptions` implementations
- Integration-test batch configurations and README examples

## Findings

### P1. `QuerydslNoOffsetPagingItemReader` is not restart-safe

`QuerydslNoOffsetPagingItemReader#createQuery` initializes the keyset boundary only
when `getPage() == 0`.

- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/QuerydslNoOffsetPagingItemReader.java`
- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/options/QuerydslNoOffsetNumberOptions.java`
- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/options/QuerydslNoOffsetStringOptions.java`

On a Spring Batch restart, `AbstractPagingItemReader` restores the item index and
jumps to the restored page. The no-offset option state (`currentId`, `lastId`) is not
stored in the `ExecutionContext`, so a restarted reader can resume with `page > 0`
and `currentId == null`. In that case `createQuery` returns the original query,
which can re-read from the beginning or skip data depending on the current page
state.

Recommended direction:

- Store keyset state in `ExecutionContext` and restore it on `open`.
- Or explicitly set `saveState(false)` / document no restart support for this reader.
- Add restart tests that fail after a partially processed chunk and verify no
  duplicate or missing rows.

### P1. The no-offset keyset predicate assumes a unique and stable sort key

The current no-offset query appends a single-column predicate such as
`where key > currentKey order by key`.

This is safe only when the key is unique, non-null, immutable during the job, and
matches the effective order of the SQL result. If a caller uses a non-unique field
such as category, name, or date, rows sharing the same key value at a page boundary
can be skipped. If the user query already has `orderBy`, Querydsl appends the no-offset
ordering after the existing order, so the actual ordering can diverge from the
keyset predicate.

Recommended direction:

- Restrict no-offset options to a unique monotonic key by default.
- For non-unique sort fields, support tuple keyset pagination, for example
  `(sortKey, id) > (:sortKey, :id)`.
- Fail fast when the user query already contains incompatible `orderBy`.
- Document that the key must be indexed and stable for the full job run.

### P1. Offset paging examples omit deterministic `orderBy`

`QuerydslPagingItemReader` uses offset/limit, but the README and integration batch
configuration examples do not add an `orderBy` clause.

- `README.md`
- `spring-batch-querydsl-integration-test/src/main/java/org/springframework/batch/item/querydsl/integrationtest/job/QuerydslPagingItemReaderConfiguration.java`

SQL does not guarantee row order without `order by`. For batch paging, that means
large jobs can duplicate or skip rows across pages, especially when rows are inserted,
updated, or execution plans change.

Recommended direction:

- Add a stable `orderBy` to all paging examples.
- Prefer ordering by a unique indexed column such as the primary key.
- Consider validation or documentation that offset paging requires deterministic
  ordering.

### P2. The `transacted` default conflicts with the documented batch-fetch intent

The current constructor defaults to `transacted = true`. In that mode the reader
starts an independent JPA transaction for each page, flushes and clears the reader
`EntityManager`, and commits after fetching.

This differs from the project article's stated motivation of relying on the chunk
transaction so Hibernate batch fetching can work naturally inside the processing
chunk. It also means entities remain managed by the reader `EntityManager` while
being processed. If processor code mutates a read entity, the next reader flush can
write those mutations unexpectedly.

Recommended direction:

- Align the default constructor, README, and article with one clear behavior.
- If `transacted = true` remains the default, consider detaching fetched entities
  before handing them to the processor.
- If chunk transaction participation is the intended default, set the sample code
  to `transacted = false` explicitly and explain the consistency trade-off.

### P2. Fetch join plus paging is not guarded

The readers apply `limit` to queries supplied by users. If a user query contains a
collection fetch join, Hibernate can paginate in memory or produce invalid SQL/JPQL
combinations when the query is cloned and converted to min/max key lookup.

Recommended direction:

- Explicitly disallow collection fetch joins in paging readers.
- Recommend `hibernate.default_batch_fetch_size` or `@BatchSize` for collection
  loading in batch jobs.
- Add tests that prove a collection fetch join is rejected or documented as unsupported.

### P2. DTO projections can fail when `transacted = false`

`fetchQuery` detaches every fetched result when `transacted = false`.

That is valid for entities, but Querydsl readers are often useful for DTO or Tuple
projections. Calling `EntityManager#detach` on a non-entity can throw an exception.

Recommended direction:

- Call `entityManager.contains(entity)` before detaching.
- Or clearly document the reader as entity-result only.
- Add a DTO projection test for both transaction modes.

## Verification

I tried to run `./gradlew test`.

- With the default JDK 25, Gradle 6.7.1 failed while compiling `settings.gradle`
  with `Unsupported class file major version 69`.
- With the installed JDK 11, build configuration failed because
  `org.ajoberstar.grgit:grgit-core:3.1.1` could not be resolved from the plugin
  repository.

No production code was changed as part of this review PR.
