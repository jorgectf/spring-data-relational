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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jdbc.core.convert.sqlgeneration.SqlAssert.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for SqlAssert
 */
class SqlAssertUnitTests {
	@Test
	void givesProperNullPointerExceptionWhenSqlIsNull() {
		assertThatThrownBy(() -> SqlAssert.assertThatParsed(null)).isInstanceOf(NullPointerException.class);
	}
	@Nested
	class AssertWhereClause {
		@Test
		void assertWhereClause() {
			SqlAssert.assertThatParsed("select x from t where z > y").hasWhere("z > y");
		}

			@Test
		void asserWhereClauseFailure() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x from t where z > y");

			assertThatThrownBy(() -> sqlAssert.hasWhere("z = y")) //
					.hasMessageContaining("z = y") //
					.hasMessageContaining("z > y");
		}

		@Test
		void asserWhereClauseFailureNoWhereClause() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x from t");

			assertThatThrownBy(() -> sqlAssert.hasWhere("z = y")) //
					.hasMessageContaining("z = y") //
					.hasMessageContaining("no where clause");
		}

	}
	@Nested
	class AssertColumns {
		@Test
		void matchingSimpleColumns() {
			SqlAssert.assertThatParsed("select x, y, z from t").hasExactlyColumns("x", "y", "z");
		}

		@Test
		void extraSimpleColumn() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x, y, z, a from t");

			assertThatThrownBy(() -> sqlAssert.hasExactlyColumns("x", "y", "z")) //
					.hasMessageContaining("x, y, z") //
					.hasMessageContaining("x, y, z, a") //
					.hasMessageContaining("a");
		}

		@Test
		void missingSimpleColumn() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x, y, z from t");

			assertThatThrownBy(() -> sqlAssert.hasExactlyColumns("x", "y", "z", "a")) //
					.hasMessageContaining("x, y, z") //
					.hasMessageContaining("x, y, z, a") //
					.hasMessageContaining("a");
		}

		@Test
		void wrongSimpleColumn() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x, y, z from t");

			assertThatThrownBy(() -> sqlAssert.hasExactlyColumns("x", "a", "z")) //
					.hasMessageContaining("x, y, z") //
					.hasMessageContaining("x, a, z") //
					.hasMessageContaining("a") //
					.hasMessageContaining("y");
		}

		@Test
		void matchesFullyQualifiedColumn() {
			SqlAssert.assertThatParsed("select t.x from t").hasExactlyColumns("x");
		}

	}

	@Nested
	class AssertAliases {
		@Test
		void simpleColumnMatchesWithAlias() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x as a from t");

			sqlAssert.hasExactlyColumns("x");
		}

		@Test
		void matchWithAlias() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x as a from t");

			sqlAssert.hasExactlyColumns(col("x").as("a"));
		}

		@Test
		void matchWithWrongAlias() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select x as b from t");

			assertThatThrownBy(() -> sqlAssert.hasExactlyColumns(col("x").as("a"))) //
					.hasMessageContaining("x as a") //
					.hasMessageContaining("x AS b");
		}
	}

	@Nested
	class AssertSubSelects {
		@Test
		void subselectGetsFound() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select a from (select x as a from t) s");

			sqlAssert //
					.hasInlineViewSelectingFrom("t") //
					.hasExactlyColumns(col("x").as("a"));
		}

		@Test
		void subselectWithWrongTableDoesNotGetFound() {

			SqlAssert sqlAssert = SqlAssert.assertThatParsed("select a from (select x as a from u) s");

			assertThatThrownBy(() -> sqlAssert //
					.hasInlineViewSelectingFrom("t"))
							.hasMessageContaining("is expected to contain a subselect selecting from t but doesn't");
		}
	}
}
