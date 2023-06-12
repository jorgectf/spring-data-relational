/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.jdbc.core.convert.sqlgeneration;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.IdGeneration;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.dialect.PostgresDialect;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.IdentifierProcessing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jdbc.core.convert.sqlgeneration.SqlAssert.*;

/**
 * Tests for {@link SingleQuerySqlGenerator}.
 * 
 * @author Jens Schauder
 */
class SingleQuerySqlGeneratorUnitTests {

	JdbcMappingContext context = new JdbcMappingContext();
	Dialect dialect = createDialect();

	@Test
	void createSelectForNoReference() {

		SingleQuerySqlGenerator sqlGenerator = new SingleQuerySqlGenerator(context, dialect, context.getRequiredPersistentEntity(SimpleEntity.class));
		AliasFactory aliases = sqlGenerator.getAliasFactory();

		String sql = sqlGenerator.findAll();

		/**
		 * select simple_entity_rn, id-alias, name-alias from (select 1 as simple_entity_rn, id as id-alias, name as name-alias from simple_entity) as table-alias
		 */

		assertThatParsed(sql) //
				.hasExactlyColumns( //
						aliases.getRowNumberAlias(path()), //
						aliases.getAlias(path( "id")), //
						aliases.getAlias(path( "name")) //
				) //
				.hasInlineViewSelectingFrom("\"simple_entity\"") //
				.hasExactlyColumns( //
						lit(1).as(aliases.getRowNumberAlias(path())) , //
						col("\"id\"").as(aliases.getAlias(path( "id"))), //
						col("\"name\"").as(aliases.getAlias(path( "name"))) //
				);
	}

	private PersistentPropertyPathExtension path() {
		return new PersistentPropertyPathExtension(context,context.getRequiredPersistentEntity(SimpleEntity.class));
	}

	private  PersistentPropertyPathExtension path(String pathAsString) {

		PersistentPropertyPath<RelationalPersistentProperty> persistentPropertyPath = context.getPersistentPropertyPath(pathAsString, SimpleEntity.class);
		return new PersistentPropertyPathExtension(context, persistentPropertyPath);
	}

	private static Dialect createDialect() {

		return PostgresDialect.INSTANCE;
	}

	record SimpleEntity(@Id Long id, String name) {
	}

}
