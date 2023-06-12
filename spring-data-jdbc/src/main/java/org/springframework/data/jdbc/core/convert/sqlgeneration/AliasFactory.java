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

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AliasFactory {
	private final Map<PersistentPropertyPathExtension, String> cache = new ConcurrentHashMap<>();
	private int counter = 0;

	public String getAlias(PersistentPropertyPathExtension path) {
		return cache.computeIfAbsent(path, p -> createAlias(p));
	}

	private String createAlias(PersistentPropertyPathExtension path) {

		counter++;

		String prefix = path.isEntity() ? "T_" : "C_";

		String name = path.isEntity() ? path.getTableName().getReference() : path.getColumnName().getReference();

		return prefix + name + "_" + counter;

	}

	public String getKeyAlias(PersistentPropertyPath<RelationalPersistentProperty> path) {
		return null;
	}

	public String getRowNumberAlias(PersistentPropertyPathExtension path) {
		if (path.getLength() == 0) {
			return "RN_root";
		}
		return "RN_" + path.getTableName();
	}
}
