/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 25, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.VariableType.VariableValue;

/**
 * Collects {@link VariableType VariableTypes} from the corresponding extension point and provides them to the
 * framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class VariableTypeRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(VariableTypeRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.FlowVariableType";

    private static final VariableTypeRegistry INSTANCE = createInstance();

    private static VariableTypeRegistry createInstance() {
        final IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(EXT_POINT_ID);
        final Map<String, VariableType<?>> variableTypes = Stream.of(point.getExtensions())
            .flatMap(e -> Stream.of(e.getConfigurationElements())).map(VariableTypeRegistry::readVariableType)
            .filter(Objects::nonNull).collect(Collectors.toMap(VariableType::getIdentifier, Function.identity(),
                VariableTypeRegistry::handleConflictingIdentifiers, LinkedHashMap::new));
        // TODO do we need some kind of sorting to make sure our types are first?
        // (In that case it might be easier to not provide them via the extension point)
        return new VariableTypeRegistry(variableTypes);
    }

    private static VariableType<?> handleConflictingIdentifiers(final VariableType<?> first,
        final VariableType<?> second) {
        LOGGER.debugWithFormat(
            "Conflicting VariableType identifier '%s' detected. "
            + "Only the VariableType '%s' is taken, while the VariableType '%s' is dropped.",
            first.getIdentifier(), first.getClass().getName(), second.getClass().getName());
        return first;
    }

    private static VariableType<?> readVariableType(final IConfigurationElement cfe) {
        try {
            final VariableTypeExtension extension = (VariableTypeExtension)cfe.createExecutableExtension("extension");
            final VariableType<?> t = extension.getVariableType();
            LOGGER.debugWithFormat("Added flow variable type '%s' from '%s'", t.getClass().getName(),
                cfe.getContributor().getName());
            return t;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s", VariableType.class.getName(),
                cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

    /**
     * Maps identifier -> VariableType.
     */
    private final Map<String, VariableType<?>> m_variableTypes;

    private VariableTypeRegistry(final Map<String, VariableType<?>> variableTypes) {
        m_variableTypes = variableTypes;
    }

    public static VariableTypeRegistry getInstance() {
        return INSTANCE;
    }

    VariableValue<?> loadValue(final NodeSettingsRO sub) throws InvalidSettingsException {
        final String identifier = CheckUtils.checkSettingNotNull(sub.getString("class"), "'class' must not be null");
        final VariableType<?> type = m_variableTypes.get(identifier);
        CheckUtils.checkSetting(type != null, "No flow variable type for identifier/class '%s'", identifier);
        @SuppressWarnings("null") // the above check ensures that type is not null
        final VariableValue<?> value = type.loadValue(sub);
        return value;
    }

    VariableType<?>[] getAllTypes() {
        return m_variableTypes.values().toArray(new VariableType[0]);
    }

}
