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

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.AliasedExpression;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.InlineQuery;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SelectBuilder;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

public class SingleQuerySqlGenerator {

	private final RelationalMappingContext context;
	private final Dialect dialect;
	private final RelationalPersistentEntity<?> aggregate;

	private final AliasFactory aliases = new AliasFactory();

	public SingleQuerySqlGenerator(RelationalMappingContext context, Dialect dialect,
			RelationalPersistentEntity<?> aggregate) {

		this.context = context;
		this.dialect = dialect;
		this.aggregate = aggregate;
	}

	public String findAll() {
		return createSelect();
	}

	public String findById() {
		return createSelect();
	}

	public String findAllById() {
		return createSelect();
	}

	private String createSelect() {

		InlineQuery inlineQuery = createInlineQuery();

		PersistentPropertyPaths<?, RelationalPersistentProperty> paths = context
				.findPersistentPropertyPaths(aggregate.getType(), p -> true);

		List<Expression> columns = new ArrayList<>();
		Expression rownumber = Expressions.just(aliases.getRowNumberAlias(new PersistentPropertyPathExtension(context, aggregate)));
		columns.add(rownumber);
		
		for (PersistentPropertyPath<RelationalPersistentProperty> ppp : paths) {

			PersistentPropertyPathExtension path = new PersistentPropertyPathExtension(context, ppp);

			columns.add(Expressions.just(aliases.getAlias(path)));
		}

		SelectBuilder.SelectFromAndJoin select = StatementBuilder.select(columns).from(inlineQuery);

		return SqlRenderer.create(new RenderContextFactory(dialect).createRenderContext()).render(select.build());
	}

	private InlineQuery createInlineQuery() {

		Table table = Table.create(aggregate.getTableName());

		PersistentPropertyPaths<?, RelationalPersistentProperty> paths = context
				.findPersistentPropertyPaths(aggregate.getType(), p -> true);

		List<Expression> columns = new ArrayList<>();
		Expression rownumber = new AliasedExpression(SQL.literalOf(1), aliases.getRowNumberAlias(new PersistentPropertyPathExtension(context, aggregate)));

		columns.add(rownumber);
		for (PersistentPropertyPath<RelationalPersistentProperty> ppp : paths) {

			PersistentPropertyPathExtension path = new PersistentPropertyPathExtension(context, ppp);

			columns.add(table.column(path.getColumnName()).as(aliases.getAlias(path)));
		}

		SelectBuilder.SelectFromAndJoin select = StatementBuilder.select(columns).from(table);

		InlineQuery inlineQuery = InlineQuery.create(select.build(), aliases.getAlias(new PersistentPropertyPathExtension(context, aggregate)));
		return inlineQuery;
	}

	public AliasFactory getAliasFactory() {
		return aliases;
	}
}
