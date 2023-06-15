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

package org.springframework.data.jdbc.core.convert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.jdbc.core.convert.sqlgeneration.AliasFactory;
import org.springframework.data.jdbc.core.convert.sqlgeneration.SingleQuerySqlGenerator;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

public class AggregateReader<T> {

	private final RelationalMappingContext mappingContext;
	private final RelationalPersistentEntity<T> aggregate;
	private final AliasFactory aliasFactory;
	private final SingleQuerySqlGenerator sqlGenerator;
	private final JdbcConverter converter;
	private final NamedParameterJdbcOperations jdbcTemplate;

	AggregateReader(RelationalMappingContext mappingContext, Dialect dialect, JdbcConverter converter,
			NamedParameterJdbcOperations jdbcTemplate, RelationalPersistentEntity<T> aggregate) {

		this.mappingContext = mappingContext;

		this.aggregate = aggregate;
		this.converter = converter;
		this.jdbcTemplate = jdbcTemplate;

		this.sqlGenerator = new SingleQuerySqlGenerator(mappingContext, dialect, aggregate);
		this.aliasFactory = sqlGenerator.getAliasFactory();
	}

	public List<T> findAll() {

		String sql = sqlGenerator.findAll();

		PathToColumnMapping pathToColumn = createPathToColumnMapping(aliasFactory);
		AggregateResultSetExtractor<T> extractor = new AggregateResultSetExtractor<>(mappingContext, aggregate, converter,
				pathToColumn);

		Iterable<T> result = jdbcTemplate.query(sql, extractor);

		return (List<T>) result;
	}

	public T findById(Object id) {

		PathToColumnMapping pathToColumn = createPathToColumnMapping(aliasFactory);
		AggregateResultSetExtractor<T> extractor = new AggregateResultSetExtractor<>(mappingContext, aggregate, converter,
				pathToColumn);

		String sql = sqlGenerator.findById();

		id = converter.writeValue(id, aggregate.getIdProperty().getTypeInformation());

		Iterator<T> result = jdbcTemplate.query(sql, Map.of("id", id), extractor).iterator();

		T returnValue = result.hasNext() ? result.next() : null;

		if (result.hasNext()) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return returnValue;
	}

	public Iterable<T> findAllById(Iterable<?> ids) {

		PathToColumnMapping pathToColumn = createPathToColumnMapping(aliasFactory);
		AggregateResultSetExtractor<T> extractor = new AggregateResultSetExtractor<>(mappingContext, aggregate, converter,
				pathToColumn);

		String sql = sqlGenerator.findAllById();

		List<Object> convertedIds = new ArrayList<>();
		for (Object id : ids) {
			convertedIds.add(converter.writeValue(id, aggregate.getIdProperty().getTypeInformation()));
		}

		return jdbcTemplate.query(sql, Map.of("ids", convertedIds), extractor);
	}

	private PathToColumnMapping createPathToColumnMapping(AliasFactory aliasFactory) {
		return new PathToColumnMapping() {
			@Override
			public String column(PersistentPropertyPath<RelationalPersistentProperty> propertyPath) {

				PersistentPropertyPathExtension aliasLookUpKey = new PersistentPropertyPathExtension(mappingContext, propertyPath);

				String alias = aliasFactory.getAlias(aliasLookUpKey);
				Assert.notNull(alias, () -> "alias for >" + aliasLookUpKey + "<must not be null");
				return alias;
			}

			@Override
			public String keyColumn(PersistentPropertyPath<RelationalPersistentProperty> propertyPath) {
				return aliasFactory.getKeyAlias(propertyPath);
			}
		};
	}

}
