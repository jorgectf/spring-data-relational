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

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SpecialSubSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.Assertions;

class SqlAssert extends AbstractAssert<SqlAssert, PlainSelect> {
	private final PlainSelect actual;

	public SqlAssert(PlainSelect actual) {
		super(actual, SqlAssert.class);

		this.actual = actual;
	}

	static SqlAssert assertThatParsed(String actualSql) {

		try {
			Statement parsed = CCJSqlParserUtil.parse(actualSql);
			return new SqlAssert((PlainSelect) ((Select) parsed).getSelectBody());
		} catch (JSQLParserException e) {
			Assertions.fail("Couldn't parse '%s'".formatted(actualSql));
		}

		throw new IllegalStateException("This should be unreachable");
	}

	static LiteralPattern lit(Object value) {
		return new LiteralPattern(value);
	}

	static ColumnPattern col(String columnName) {
		return new SimpleColumnPattern(columnName);
	}

	SqlAssert hasExactlyColumns(String... columns) {

		ColumnPattern[] patterns = new ColumnPattern[columns.length];

		for (int i = 0; i < columns.length; i++) {
			patterns[i] = col(columns[i]);
		}

		return hasExactlyColumns(patterns);
	}

	SqlAssert hasExactlyColumns(ColumnPattern... columns) {

		List<SelectItem> actualSelectItems = actual.getSelectItems();
		List<ColumnPattern> unmatchedPatterns = new ArrayList<>(Arrays.asList(columns));
		List<SelectItem> unmatchedSelectItems = new ArrayList<>();

		for (SelectItem selectItem : actualSelectItems) {

			ColumnPattern matchedPattern = null;
			for (ColumnPattern column : unmatchedPatterns) {
				if (column.matches(selectItem)) {
					matchedPattern = column;
					break;
				}
			}

			if (matchedPattern != null) {
				unmatchedPatterns.remove(matchedPattern);
			} else {
				unmatchedSelectItems.add(selectItem);
			}
		}

		if (unmatchedPatterns.isEmpty() && unmatchedSelectItems.isEmpty()) {
			return this;
		}

		String preparedExpectedColumns = prepare(columns);

		if (unmatchedPatterns.isEmpty()) {
			throw failureWithActualExpected(actual, preparedExpectedColumns, """
					Expected
					  %s
					to select the columns
					  %s
					but
					  %s
					  were not expected
					""", actual, preparedExpectedColumns, unmatchedSelectItems);
		}
		if (unmatchedSelectItems.isEmpty()) {
			throw failureWithActualExpected(actual, preparedExpectedColumns, """
					Expected
					  %s
					to select the columns
					  %s
					but
					  %s
					  were not present
					""", actual, preparedExpectedColumns, unmatchedPatterns);
		}
		throw failureWithActualExpected(actual, preparedExpectedColumns, """
				Expected
				  %s
				to select the columns
				  %s
				but
				  %s
				  were not present and
				  %s
				  were not expected""", actual, preparedExpectedColumns, unmatchedPatterns, unmatchedSelectItems);
	}

	public SqlAssert hasWhere(String whereClause) {

		Expression where = actual.getWhere();

		if (where == null) {
			throw failureWithActualExpected(actual, "a query containing " + whereClause,
					"expected %s to have the where clause %s but it has no where clause", actual, whereClause);
		}

		if (!where.toString().equals(whereClause)) {
			throw failureWithActualExpected(actual, "a query containing " + whereClause,
					"expected %s to have the where clause %s", actual, whereClause);
		}

		return this;
	}

	private String prepare(ColumnPattern[] columns) {
		return Arrays.toString(columns);
	}

	SqlAssert hasInlineViewSelectingFrom(String tableName) {

		Optional<PlainSelect> matchingSelect = getSubSelects(actual)
				.filter(ps -> (ps.getFromItem()instanceof Table t) && t.getName().equals(tableName)).findFirst();

		if (matchingSelect.isEmpty()) {
			throw failureWithActualExpected(actual, "Subselect from " + tableName,
					"%s is expected to contain a subselect selecting from %s but doesn't", actual, tableName);
		}

		return new SqlAssert(matchingSelect.get());
	}

	private static Stream<PlainSelect> getSubSelects(PlainSelect select) {

		FromItem fromItem = select.getFromItem();

		Stream<PlainSelect> fromStream = subSelects(fromItem);

		return Stream.of(select).flatMap(s -> {
			List<Join> joins = s.getJoins();
			if (joins == null) {
				return fromStream;
			}

			Stream<PlainSelect> joinStream = joins.stream() //
					.map(j -> j.getRightItem()) //
					.flatMap(ss -> subSelects(ss));
			return Stream.concat(fromStream, joinStream);
		});
	}

	private static Stream<PlainSelect> subSelects(FromItem fromItem) {
		Stream<PlainSelect> fromStream;
		if (fromItem instanceof SubSelect ss) {
			fromStream = Stream.of((PlainSelect) ss.getSelectBody());
		} else if (fromItem instanceof SpecialSubSelect ss) {
			fromStream = Stream.of((PlainSelect) ss.getSubSelect().getSelectBody());
		} else {
			fromStream = Stream.empty();
		}
		return fromStream;
	}

}
