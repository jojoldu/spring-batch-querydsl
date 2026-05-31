# Toby Hexagonal Architecture Based Code Review

This review applies the perspective from Toby's Clean Spring course on domain model
patterns and hexagonal architecture. The project is not a business application, so
the "domain" here is not a user-facing business domain. The core domain of this
library is the paging reader policy itself: page state, keyset/no-offset rules,
restart behavior, and query execution boundaries.

## Scope

- `spring-batch-querydsl-reader`
- `spring-batch-querydsl-integration-test`
- README examples that show the intended public API

## Review Lens

The main review questions were:

- Is the core policy protected from external technology details?
- Are ports and adapters separated enough to make behavior easy to test?
- Does the Spring Batch adapter lifecycle match the stateful nature of the reader?
- Are package boundaries explicit enough to be protected by architecture tests?
- Does the sample code communicate the intended architecture clearly?

## Findings

### P1. Reader beans are singleton-scoped even though the readers hold execution state

`QuerydslPagingItemReader` owns mutable execution resources and state such as
`EntityManager`, `results`, and `transacted`.

- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/QuerydslPagingItemReader.java`

`QuerydslNoOffsetPagingItemReader` additionally owns `QuerydslNoOffsetOptions`, which
keeps keyset state such as `currentId` and `lastId`.

- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/QuerydslNoOffsetPagingItemReader.java`
- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/options/QuerydslNoOffsetNumberOptions.java`
- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/options/QuerydslNoOffsetStringOptions.java`

However, the integration-test job configurations expose the readers as plain
singleton `@Bean`s.

- `spring-batch-querydsl-integration-test/src/main/java/org/springframework/batch/item/querydsl/integrationtest/job/QuerydslPagingItemReaderConfiguration.java`
- `spring-batch-querydsl-integration-test/src/main/java/org/springframework/batch/item/querydsl/integrationtest/job/QuerydslNoOffsetPagingItemReaderConfiguration.java`

From a hexagonal architecture perspective, this is an adapter lifecycle problem.
The Batch adapter is stateful per step execution, but the Spring bean lifecycle does
not express that boundary. A repeated job execution in the same application context
can reuse reader and option state unexpectedly.

Recommended direction:

- Declare reader beans as `@StepScope`.
- Create `QuerydslNoOffsetOptions` inside the step-scoped reader factory.
- Add tests that launch the same job twice in one Spring context and verify that
  no option state leaks between executions.

### P1. The core no-offset policy is tightly coupled to Querydsl and JPA query objects

The no-offset options both decide the paging policy and mutate `JPAQuery` directly.

- `QuerydslNoOffsetOptions#initKeys`
- `QuerydslNoOffsetOptions#createQuery`
- `QuerydslNoOffsetNumberOptions#createQuery`
- `QuerydslNoOffsetStringOptions#createQuery`

In hexagonal terms, the keyset/no-offset algorithm is core behavior, while
`JPAQuery`, `NumberPath`, `StringPath`, and Querydsl ordering are adapter details.
Because they are merged, the keyset rules cannot be tested without Querydsl/JPA, and
supporting another query mechanism would require rewriting the core algorithm.

Recommended direction:

- Extract a small core policy object that tracks the current key and decides the
  next key boundary independently from Querydsl.
- Keep Querydsl-specific path and predicate construction in a Querydsl adapter.
- Unit test the core policy without Spring, JPA, Hibernate, or Querydsl.

### P1. Reflection-based key extraction pierces the model boundary

`QuerydslNoOffsetOptions#getFiledValue` derives a Java field name from a Querydsl
path string and uses reflection on the fetched item.

- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/options/QuerydslNoOffsetOptions.java`

This makes the paging policy depend on entity field names and access shape. It can
break for inherited fields, embedded values, DTO projections, property access, or
any model where the Querydsl path does not map cleanly to a declared Java field.
It also forces the core policy to know how to inspect the result object.

Recommended direction:

- Accept an explicit key extractor, for example `Function<T, K> keyExtractor`.
- Keep the Querydsl path for query construction, but use the extractor for result
  state updates.
- Fix the typo from `getFiledValue` to `getFieldValue` if the reflection path remains.

### P2. The duplicated no-offset readers blur the intended abstraction

`QuerydslNoOffsetPagingItemReader` and `QuerydslNoOffsetIdPagingItemReader` contain
nearly identical logic.

- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/QuerydslNoOffsetPagingItemReader.java`
- `spring-batch-querydsl-reader/src/main/java/org/springframework/batch/item/querydsl/reader/QuerydslNoOffsetIdPagingItemReader.java`

The difference is mostly the option type, not the reader behavior. This makes the
public API harder to understand and increases the chance that fixes are applied to
one reader but not the other.

Recommended direction:

- Keep a single no-offset reader implementation.
- Move ID-specific convenience into factory methods or option builders.
- Add deprecation guidance if both public classes are already released.

### P2. The integration-test sample does not show clear ports and adapters

The sample job directly connects Spring Batch configuration, Querydsl reader setup,
JPA writer setup, and entity conversion in one configuration class.

- `QuerydslPagingItemReaderConfiguration`
- `QuerydslNoOffsetPagingItemReaderConfiguration`

This is acceptable for a compact integration test, but the README and sample code are
also the public teaching surface of the library. If users copy the sample into a real
application, the application use case, batch inbound adapter, and persistence outbound
adapter can collapse into one class.

Recommended direction:

- Keep the current integration tests if they are only smoke tests.
- Add one small example package that names the boundaries explicitly:
  `application`, `application.port.in`, `application.port.out`, `adapter.batch`,
  and `adapter.persistence`.
- Put the `ItemProcessor` mapping behind an application use case so the batch step
  orchestrates rather than owns business behavior.

### P2. JPA entity and domain model separation should be documented as a deliberate choice

The sample uses JPA entities directly as the model.

- `spring-batch-querydsl-integration-test/src/main/java/org/springframework/batch/item/querydsl/integrationtest/entity/Manufacture.java`
- `spring-batch-querydsl-integration-test/src/main/java/org/springframework/batch/item/querydsl/integrationtest/entity/ManufactureBackup.java`

This is not automatically wrong. Toby's course emphasizes a practical judgment:
JPA entities can be clean domain objects when the domain model and persistence model
are aligned. But that decision should be explicit, especially because the package is
named `entity` and the sample uses the entity directly in batch processing.

Recommended direction:

- Document that the sample intentionally uses JPA entities as the domain model because
  the model is simple and persistence shape matches the domain shape.
- For more complex examples, show when to split domain objects from persistence
  entities and connect them through an adapter.

### P2. Architecture rules are not executable

The project does not include architecture tests that protect dependencies between
core reader logic, Querydsl/JPA adapters, and integration-test sample code.

The course material highlights that architecture is sustained by tests and code
verification, not only package naming or diagrams. In this project, an ArchUnit test
could prevent accidental dependencies in the wrong direction.

Recommended direction:

- Add ArchUnit tests for package boundaries.
- Make it explicit that `spring-batch-querydsl-reader` must not depend on
  `spring-batch-querydsl-integration-test`.
- If a core policy package is extracted, enforce that it does not depend on JPA,
  Hibernate, Spring Batch, or Querydsl.

## Suggested Refactoring Path

1. Make sample reader beans `@StepScope` and add repeated execution tests.
2. Replace reflection-based key extraction with an explicit `Function<T, K>`.
3. Extract keyset state and boundary decisions into a small, dependency-free policy.
4. Keep Querydsl predicate mutation in a dedicated adapter around that policy.
5. Collapse duplicate no-offset reader classes or deprecate the redundant one.
6. Add architecture tests that make the new boundaries executable.

## Verification

I did not change production code in this PR. This is a review document only.

The previous test attempt for this checkout failed before test execution:

- With the default JDK 25, Gradle 6.7.1 failed with
  `Unsupported class file major version 69`.
- With the installed JDK 11, build configuration failed because
  `org.ajoberstar.grgit:grgit-core:3.1.1` could not be resolved from the plugin
  repository.
