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
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

public class SingleQuerySqlGenerator {

	private final RelationalMappingContext context;
	private final Dialect dialect;
	private final AliasFactory aliases = new AliasFactory();

	private final RelationalPersistentEntity<?> aggregate;
	private final Table table;

	public SingleQuerySqlGenerator(RelationalMappingContext context, Dialect dialect,
			RelationalPersistentEntity<?> aggregate) {

		this.context = context;
		this.dialect = dialect;
		this.aggregate = aggregate;

		this.table = Table.create(aggregate.getTableName());
	}

	public String findAll() {
		return createSelect(null);
	}

	public String findById() {

		PersistentPropertyPathExtension idPPPE = new PersistentPropertyPathExtension(context, aggregate).extendBy(aggregate.getIdProperty());
		Condition condition = Conditions.isEqual(
				table.column(idPPPE.getColumnName()),
				Expressions.just(":id"));
		return createSelect(condition);
	}

	public String findAllById() {
		return createSelect(null);
	}

	private String createSelect(Condition condition) {

		InlineQuery inlineQuery = createInlineQuery(condition);

		PersistentPropertyPaths<?, RelationalPersistentProperty> paths = context
				.findPersistentPropertyPaths(aggregate.getType(), p -> true);

		List<Expression> columns = new ArrayList<>();
		Expression rownumber = Expressions
				.just(aliases.getRowNumberAlias(new PersistentPropertyPathExtension(context, aggregate)));
		columns.add(rownumber);

		for (PersistentPropertyPath<RelationalPersistentProperty> ppp : paths) {

			PersistentPropertyPathExtension path = new PersistentPropertyPathExtension(context, ppp);

			columns.add(Expressions.just(aliases.getAlias(path)));
		}

		SelectBuilder.SelectFromAndJoin select = StatementBuilder.select(columns).from(inlineQuery);

		return SqlRenderer.create(new RenderContextFactory(dialect).createRenderContext()).render(select.build());
	}

	private InlineQuery createInlineQuery(Condition condition) {

		PersistentPropertyPaths<?, RelationalPersistentProperty> paths = context
				.findPersistentPropertyPaths(aggregate.getType(), p -> true);

		List<Expression> columns = new ArrayList<>();
		Expression rownumber = new AliasedExpression(SQL.literalOf(1),
				aliases.getRowNumberAlias(new PersistentPropertyPathExtension(context, aggregate)));

		columns.add(rownumber);
		for (PersistentPropertyPath<RelationalPersistentProperty> ppp : paths) {

			PersistentPropertyPathExtension path = new PersistentPropertyPathExtension(context, ppp);

			columns.add(table.column(path.getColumnName()).as(aliases.getAlias(path)));
		}

		SelectBuilder.SelectWhere select = StatementBuilder.select(columns).from(table);

		SelectBuilder.BuildSelect buildSelect = condition != null ? select.where(condition) : select;

		InlineQuery inlineQuery = InlineQuery.create(buildSelect.build(),
				aliases.getAlias(new PersistentPropertyPathExtension(context, aggregate)));
		return inlineQuery;
	}

	public AliasFactory getAliasFactory() {
		return aliases;
	}
}
