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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

public class AliasFactory {
	private final Map<PersistentPropertyPathExtension, String> cache = new ConcurrentHashMap<>();
	private int counter = 0;

	private static String sanatize(String name) {
		return name.replaceAll("\\W", "");
	}

	public String getAlias(PersistentPropertyPathExtension path) {
		return cache.computeIfAbsent(path, this::createAlias);
	}

	private String createAlias(PersistentPropertyPathExtension path) {

		String prefix = path.isEntity() ? "t_" : "c_";

		String name = getName(path);

		return prefix + name + "_" + ++counter;

	}

	public String getKeyAlias(PersistentPropertyPath<RelationalPersistentProperty> path) {
		return null;
	}

	public String getRowNumberAlias(PersistentPropertyPathExtension path) {
		return cache.computeIfAbsent(path, this::createRowNumberAlias);

	}
	public String createRowNumberAlias(PersistentPropertyPathExtension path) {
		return "rn_" + getName(path) + "_" + ++counter;
	}

	private static String getName(PersistentPropertyPathExtension path) {
		return sanatize( //
				path.isEntity() //
						? path.getTableName().getReference() //
						: path.getColumnName().getReference()) //
				.toLowerCase();
	}
}
