package org.springframework.batch.item.querydsl.integrationtest.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FooRepository extends JpaRepository<Foo, Long> {
}