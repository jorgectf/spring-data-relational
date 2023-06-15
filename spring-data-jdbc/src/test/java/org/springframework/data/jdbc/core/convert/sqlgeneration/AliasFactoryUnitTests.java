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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

class AliasFactoryUnitTests {

	RelationalMappingContext context = new RelationalMappingContext();
	AliasFactory aliasFactory = new AliasFactory();

	@Nested
	class SimpleAlias {
	@Test
	void aliasForRoot() {

		String alias = aliasFactory
				.getAlias(new PersistentPropertyPathExtension(context, context.getRequiredPersistentEntity(DummyEntity.class)));

		assertThat(alias).isEqualTo("t_dummy_entity_1");
	}

	@Test
	void aliasSimpleProperty() {

		String alias = aliasFactory.getAlias(
				new PersistentPropertyPathExtension(context, context.getPersistentPropertyPath("name", DummyEntity.class)));

		assertThat(alias).isEqualTo("c_name_1");
	}

	@Test
	void nameGetsSanatized() {

		String alias = aliasFactory.getAlias(
				new PersistentPropertyPathExtension(context, context.getPersistentPropertyPath("evil", DummyEntity.class)));

		assertThat(alias).isEqualTo("c_ameannamecontains3illegal_characters_1");
	}

	@Test
	void aliasIsStable() {

		String alias1 = aliasFactory
				.getAlias(new PersistentPropertyPathExtension(context, context.getRequiredPersistentEntity(DummyEntity.class)));
		String alias2 = aliasFactory
				.getAlias(new PersistentPropertyPathExtension(context, context.getRequiredPersistentEntity(DummyEntity.class)));

		assertThat(alias1).isEqualTo(alias2);
	}
	}
	@Nested
	class RnAlias {

		@Test
		void aliasIsStable() {

			String alias1 = aliasFactory
					.getRowNumberAlias(new PersistentPropertyPathExtension(context, context.getRequiredPersistentEntity(DummyEntity.class)));
			String alias2 = aliasFactory
					.getRowNumberAlias(new PersistentPropertyPathExtension(context, context.getRequiredPersistentEntity(DummyEntity.class)));

			assertThat(alias1).isEqualTo(alias2);
		}
	}
	static class DummyEntity {
		String name;

		@Column("a mean name <-- contains > 3 illegal_characters.") String evil;
	}
}
