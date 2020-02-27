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
 *   Apr 28, 2019 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.swing.Icon;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue;

import com.google.common.collect.Sets;

/**
 * The type of a {@link FlowVariable}, replacing {@link FlowVariable.Type}. By convention, subclasses of this type are
 * singletons. The list of these singleton subclasses is not API and may change between versions of KNIME. To create a
 * new {@link FlowVariable} of a certain type, use {@link FlowVariable#FlowVariable(String, VariableType, Object)}.
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The simple value type that is used by clients (e.g. String, Double, Integer)
 * @since 4.1
 */
public abstract class VariableType<T> {

    private static final String CFG_CLASS = "class";

    private static final String CFG_VALUE = "value";

    /**
     * The value of a {@link FlowVariable}. Associates a simple value (e.g. String, Double, Integer) with a {@link VariableType}.
     *
     * @noextend This class is not intended to be subclassed by clients.
     *
     * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
     * @author Marc Bux, KNIME GmbH, Berlin, Germany
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> The simple value type that is usded by clients (e.g. String, Double, Integer)
     * @since 4.2
     */
    protected abstract static class VariableValue<T> {

        private final VariableType<T> m_type;

        private final T m_value;

        /**
         * Constructor.
         *
         * @param type of the variable
         * @param value of the variable
         */
        protected VariableValue(final VariableType<T> type, final T value) {
            m_type = CheckUtils.checkArgumentNotNull(type);
            m_value = value;
        }

        String asString() {
            return m_value.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final VariableValue<?> other = (VariableValue<?>)obj;
            return getType().equals(other.getType()) && Objects.equals(m_value, other.m_value);
        }

        public T get() {
            return m_value;
        }

        <U> U getAs(final VariableType<U> expectedType) {
            CheckUtils.checkArgument(m_type.getConvertibleTypes().contains(expectedType),
                "The type '%s' is incompatible with the type '%s'.", m_type, expectedType);
            return m_type.getAs(this, expectedType);
        }

        VariableType<T> getType() {
            return m_type;
        }

        @Override
        public int hashCode() {
            return 31 * m_type.hashCode() + Objects.hashCode(m_value);
        }

        void save(final NodeSettingsWO settings) {
            getType().save(this, settings);
        }

        @Override
        public String toString() {
            return asString();
        }

    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Boolean} values. The singleton instance is accessible
     * via the {@link BooleanType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class BooleanType extends VariableType<Boolean> {

        private static final class BooleanValue extends VariableValue<Boolean> {

            private BooleanValue(final Boolean i) {
                super(INSTANCE, i);
            }
        }

        /**
         * Used to register {@link BooleanType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class BooleanTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return BooleanType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link BooleanType} type.
         */
        public static final BooleanType INSTANCE = new BooleanType();

        private BooleanType() {
            //singleton
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_BOOLEAN.get();
        }

        @Override
        protected VariableValue<Boolean> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new BooleanType.BooleanValue(settings.getBoolean(CFG_VALUE));
        }

        @Override
        protected VariableValue<Boolean> newValue(final Boolean v) {
            return new BooleanType.BooleanValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Boolean> v) {
            settings.addBoolean(CFG_VALUE, v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Boolean} values. The singleton instance is
     * accessible via the {@link BooleanArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class BooleanArrayType extends VariableType<Boolean[]> {

        private static final class BooleanArrayFlowVariableValue extends VariableValue<Boolean[]> {

            private BooleanArrayFlowVariableValue(final Boolean[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * Used to register {@link BooleanArrayType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class BooleanArrayTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return BooleanArrayType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link BooleanArrayType} type.
         */
        public static final BooleanArrayType INSTANCE = new BooleanArrayType();

        private BooleanArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_BOOLEAN_ARRAY.get();
        }

        @Override
        protected VariableValue<Boolean[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            final boolean[] bools = settings.getBooleanArray(CFG_VALUE);
            return new BooleanArrayType.BooleanArrayFlowVariableValue(
                IntStream.range(0, bools.length).mapToObj(i -> bools[i]).toArray(Boolean[]::new));
        }

        @Override
        protected VariableValue<Boolean[]> newValue(final Boolean[] v) {
            return new BooleanArrayType.BooleanArrayFlowVariableValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Boolean[]> v) {
            settings.addBooleanArray(CFG_VALUE, ArrayUtils.toPrimitive(v.get()));
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Double} values. The singleton instance is accessible
     * via the {@link DoubleType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class DoubleType extends VariableType<Double> {


        private static final class DoubleValue extends VariableValue<Double> {

            private DoubleValue(final Double d) {
                super(INSTANCE, d);
            }
        }

        /**
         * Used to register {@link DoubleType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class DoubleTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return DoubleType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link DoubleType} type.
         */
        public static final DoubleType INSTANCE = new DoubleType();

        // The varargs are safe because the created array is neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE_TYPES =
            Collections.unmodifiableSet(Sets.newHashSet(INSTANCE, StringType.INSTANCE));

        private DoubleType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_DOUBLE.get();
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.DOUBLE;
        }

        @Override
        protected VariableValue<Double> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new DoubleType.DoubleValue(settings.getDouble(CFG_VALUE));
        }

        @Override
        protected VariableValue<Double> newValue(final Double v) {
            return new DoubleType.DoubleValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Double> v) {
            settings.addDouble(CFG_VALUE, v.get());
        }

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE_TYPES;
        }

        @Override
        protected <U> U getAs(final VariableValue<Double> value, final VariableType<U> convertibleType) {
            if (this.equals(convertibleType)) {
                return super.getAs(value, convertibleType);
            } else if (StringType.INSTANCE.equals(convertibleType)) {
                @SuppressWarnings("unchecked")
                final U result = (U)value.asString();
                return result;
            } else {
                throw createNotConvertibleException(this, convertibleType);
            }
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Double} values. The singleton instance is
     * accessible via the {@link DoubleArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class DoubleArrayType extends VariableType<Double[]> {

        private static final class DoubleArrayValue extends VariableValue<Double[]> {

            private DoubleArrayValue(final Double[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * Used to register {@link DoubleArrayType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class DoubleArrayTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return DoubleArrayType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link DoubleArrayType} type.
         */
        public static final DoubleArrayType INSTANCE = new DoubleArrayType();

        private DoubleArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_DOUBLE_ARRAY.get();
        }

        @Override
        protected VariableValue<Double[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new DoubleArrayType.DoubleArrayValue(
                DoubleStream.of(settings.getDoubleArray(CFG_VALUE)).boxed().toArray(Double[]::new));
        }

        @Override
        protected VariableValue<Double[]> newValue(final Double[] v) {
            return new DoubleArrayType.DoubleArrayValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Double[]> v) {
            settings.addDoubleArray(CFG_VALUE, Stream.of(v.get()).mapToDouble(Double::doubleValue).toArray());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Integer} values. The singleton instance is accessible
     * via the {@link IntType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class IntType extends VariableType<Integer> {

        private static final class IntValue extends VariableValue<Integer> {

            private IntValue(final Integer i) {
                super(INSTANCE, i);
            }
        }

        /**
         * Used to register {@link IntType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class IntTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return IntType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link IntType} type.
         */
        public static final IntType INSTANCE = new IntType();

        // Safe because the array is neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE_TYPES =
            Collections.unmodifiableSet(Sets.newHashSet(INSTANCE, DoubleType.INSTANCE, StringType.INSTANCE));

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_INTEGER.get();
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.INTEGER;
        }

        @Override
        protected VariableValue<Integer> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new IntType.IntValue(settings.getInt(CFG_VALUE));
        }

        @Override
        protected VariableValue<Integer> newValue(final Integer v) {
            return new IntType.IntValue(v);
        }

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE_TYPES;
        }

        @Override
        protected <U> U getAs(final VariableValue<Integer> value, final VariableType<U> conversionTarget) {
            if (this.equals(conversionTarget)) {
                return super.getAs(value, conversionTarget);
            } else if (DoubleType.INSTANCE.equals(conversionTarget)) {
                // Safe because U is Double in case of DoubleType
                @SuppressWarnings("unchecked")
                U result = (U)Double.valueOf(value.get().doubleValue());
                return result;
            } else if (StringType.INSTANCE.equals(conversionTarget)) {
                // TODO implement string support
                return null;
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Integer> v) {
            settings.addInt(CFG_VALUE, v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Integer} values. The singleton instance is
     * accessible via the {@link IntArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class IntArrayType extends VariableType<Integer[]> {

        private static final class IntArrayValue extends VariableValue<Integer[]> {

            private IntArrayValue(final Integer[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * Used to register {@link IntArrayType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class IntArrayTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return IntArrayType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link IntArrayType} type.
         */
        public static final IntArrayType INSTANCE = new IntArrayType();

        private IntArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_INTEGER_ARRAY.get();
        }

        @Override
        protected VariableValue<Integer[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new IntArrayType.IntArrayValue(
                IntStream.of(settings.getIntArray(CFG_VALUE)).boxed().toArray(Integer[]::new));
        }

        @Override
        protected VariableValue<Integer[]> newValue(final Integer[] v) {
            return new IntArrayType.IntArrayValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Integer[]> v) {
            settings.addIntArray(CFG_VALUE, Stream.of(v.get()).mapToInt(Integer::intValue).toArray());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Long} values. The singleton instance is accessible via
     * the {@link LongType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class LongType extends VariableType<Long> {

        private static final class LongValue extends VariableValue<Long> {

            private LongValue(final Long i) {
                super(INSTANCE, i);
            }
        }

        /**
         * Used to register {@link LongType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class LongTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return LongType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link LongType} type.
         */
        public static final LongType INSTANCE = new LongType();

        private LongType() {
            //singleton
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_LONG.get();
        }

        @Override
        protected VariableValue<Long> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new LongType.LongValue(settings.getLong(CFG_VALUE));
        }

        @Override
        protected VariableValue<Long> newValue(final Long v) {
            return new LongType.LongValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Long> v) {
            settings.addLong(CFG_VALUE, v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Long} values. The singleton instance is
     * accessible via the {@link LongArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class LongArrayType extends VariableType<Long[]> {

        private static final class LongArrayValue extends VariableValue<Long[]> {

            private LongArrayValue(final Long[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * Used to register {@link LongArrayType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class LongArrayTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return LongArrayType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link LongArrayType} type.
         */
        public static final LongArrayType INSTANCE = new LongArrayType();

        private LongArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_LONG_ARRAY.get();
        }

        @Override
        protected VariableValue<Long[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new LongArrayType.LongArrayValue(
                LongStream.of(settings.getLongArray(CFG_VALUE)).boxed().toArray(Long[]::new));
        }

        @Override
        protected VariableValue<Long[]> newValue(final Long[] v) {
            return new LongArrayType.LongArrayValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Long[]> v) {
            settings.addLongArray(CFG_VALUE, Stream.of(v.get()).mapToLong(Long::longValue).toArray());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link String} values. The singleton instance is accessible
     * via the {@link StringType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class StringType extends VariableType<String> {

        private static final class StringValue extends VariableValue<String> {

            private StringValue(final String string) {
                super(INSTANCE, string);
            }

            @Override
            String asString() {
                return get();
            }
        }

        /**
         * Used to register {@link StringType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class StringTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return StringType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link StringType} type.
         */
        public static final StringType INSTANCE = new StringType();

        private StringType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_STRING.get();
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.STRING;
        }

        @Override
        protected VariableValue<String> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new StringType.StringValue(settings.getString(CFG_VALUE));
        }

        @Override
        protected VariableValue<String> newValue(final String v) {
            return new StringType.StringValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<String> v) {
            settings.addString(CFG_VALUE, v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link String} values. The singleton instance is
     * accessible via the {@link StringArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class StringArrayType extends VariableType<String[]> {

        private static final class StringArrayValue extends VariableValue<String[]> {

            private StringArrayValue(final String[] string) {
                super(INSTANCE, string);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * Used to register {@link StringArrayType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class StringArrayTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return StringArrayType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link StringArrayType} type.
         */
        public static final StringArrayType INSTANCE = new StringArrayType();

        private StringArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_STRING_ARRAY.get();
        }

        @Override
        protected VariableValue<String[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new StringArrayType.StringArrayValue(settings.getStringArray(CFG_VALUE));
        }

        @Override
        protected VariableValue<String[]> newValue(final String[] v) {
            return new StringArrayType.StringArrayValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<String[]> v) {
            settings.addStringArray(CFG_VALUE, v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of CredentialsFlowVariableValue values. The singleton
     * instance is accessible via the {@link CredentialsType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class CredentialsType extends VariableType<CredentialsFlowVariableValue> {

        private static final class CredentialsValue extends VariableValue<CredentialsFlowVariableValue> {

            private CredentialsValue(final CredentialsFlowVariableValue c) {
                super(INSTANCE, c);
            }

            @Override
            String asString() {
                return "Credentials: " + get().getName();
            }
        }

        /**
         * Used to register {@link CredentialsType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class CredentialsTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return CredentialsType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link CredentialsType} type.
         */
        public static final CredentialsType INSTANCE = new CredentialsType();

        private CredentialsType() {
            // singleton
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.CREDENTIALS;
        }

        @Override
        protected VariableValue<CredentialsFlowVariableValue> loadValue(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            return new CredentialsType.CredentialsValue(
                CredentialsFlowVariableValue.load(settings.getNodeSettings(CFG_VALUE)));
        }

        @Override
        protected VariableValue<CredentialsFlowVariableValue> newValue(final CredentialsFlowVariableValue v) {
            return new CredentialsType.CredentialsValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<CredentialsFlowVariableValue> v) {
            v.get().save(settings.addNodeSettings(CFG_VALUE));
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link FSConnectionFlowVariableValue} values. The singleton
     * instance is accessible via the {@link FSConnectionType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class FSConnectionType extends VariableType<FSConnectionFlowVariableValue> {

        private static final class FSConnectionValue extends VariableValue<FSConnectionFlowVariableValue> {

            private FSConnectionValue(final FSConnectionFlowVariableValue c) {
                super(INSTANCE, c);
            }

            @Override
            String asString() {
                return get().connectionKey();
            }
        }

        /**
         * Used to register {@link FSConnectionType} at the FlowVariableType extension point.
         *
         * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
         * @since 4.2
         */
        public static final class FSConnectionTypeExtension implements VariableTypeExtension {

            @Override
            public VariableType<?> getVariableType() {
                return BooleanType.INSTANCE;
            }

        }

        /**
         * The singleton instance of the {@link FSConnectionType} type.
         */
        public static final FSConnectionType INSTANCE = new FSConnectionType();

        private FSConnectionType() {
            // singleton
        }

        @Override
        protected VariableValue<FSConnectionFlowVariableValue> loadValue(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            return new FSConnectionType.FSConnectionValue(
                new FSConnectionFlowVariableValue(settings.getString(CFG_VALUE)));
        }

        @Override
        protected VariableValue<FSConnectionFlowVariableValue> newValue(final FSConnectionFlowVariableValue v) {
            return new FSConnectionType.FSConnectionValue(v);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<FSConnectionFlowVariableValue> v) {
            settings.addString(CFG_VALUE, v.get().connectionKey());
        }
    }

    /**
     * Lazy-initialized array of all supported types (lazy as otherwise it causes class loading race conditions). In the
     * future this list may or may not be filled by means of an extension point.
     */
    static VariableType<?>[] getAllTypes() {
        return VariableTypeRegistry.getInstance().getAllTypes();
    }

    static VariableValue<?> load(final NodeSettingsRO sub) throws InvalidSettingsException {
        final String identifier = CheckUtils.checkSettingNotNull(sub.getString(CFG_CLASS), "'class' must not be null");
        final VariableType<?> type = Arrays.stream(getAllTypes())//
            .filter(t -> identifier.equals(t.getIdentifier()))//
            .findFirst()//
            .orElseThrow(
                () -> new InvalidSettingsException(
                    String.format("No flow variable type for identifier/class '%s'", identifier)));
        return type.loadValue(sub);
    }

    /**
     * Method for obtaining the icon associated with a {@link VariableType}.
     *
     * @return the type's icon
     */
    public Icon getIcon() {
        return SharedIcons.FLOWVAR_GENERAL.get();
    }

    /**
     * Method for obtaining the String that uniquely identifies a {@link VariableType}.
     *
     * @return the type's unique identifier
     */
    public String getIdentifier() {
        @SuppressWarnings("deprecation")
        final boolean isOtherType = getType() == FlowVariable.Type.OTHER;
        return isOtherType ? getClass().getSimpleName().replace("Type", "").toUpperCase() : getType().toString();
    }

    /**
     * Checks if this type can be converted to {@link VariableType type}, i.e. if getAs(VariableValue<T>,
     * VariableType<U>) can convert the T of VariableValue<T> to the U of VariableType<U>.
     *
     * @param type to check for convertability
     * @return <code>true</code> if <b>type</b> is compatible with this type
     * @since 4.2
     */
    public final boolean isConvertible(final VariableType<?> type) {
        return getConvertibleTypes().contains(type);
    }

    /**
     * Returns the set of {@link VariableType VariableTypes} this type can be converted to, i.e. all types for which
     * {@link VariableType#getAs(VariableValue, VariableType)} is properly defined.
     *
     * @return the set of convertible {@link VariableType VariableTypes}
     * @since 4.2
     */
    public Set<VariableType<?>> getConvertibleTypes() {
        return Collections.singleton(this);
    }

    /**
     * Converts the value stored in {@link VariableValue value} to the type of {@link VariableType compatibleType}.
     *
     * @param value to convert
     * @param conversionTarget to convert <b>value</b> to
     * @return the converted value stored in <b>value</b>
     * @since 4.2
     */
    protected <U> U getAs(final VariableValue<T> value, final VariableType<U> conversionTarget) {
        CheckUtils.checkArgumentNotNull(value);
        CheckUtils.checkArgumentNotNull(conversionTarget);
        CheckUtils.checkArgument(this.equals(conversionTarget), "The type '%s' is incompatible with the type '%s'.",
            conversionTarget, this);
        CheckUtils.checkArgument(this.equals(value.getType()), "Can't convert incompatible value '%s'.", value);
        // the above check makes sure that U is T
        @SuppressWarnings("unchecked")
        final U result = (U)value.get();
        return result;
    }

    /**
     * Creates the exception to be thrown if a user attempts to convert one flow variable type to a type it is not
     * convertible to.
     *
     * @param from the type to convert to {@link VariableType to}
     * @param to the type to convert to
     * @return the not convertible exception
     * @since 4.2
     */
    protected static IllegalArgumentException createNotConvertibleException(final VariableType<?> from,
        final VariableType<?> to) {
        return new IllegalArgumentException(String.format(
            "Flow variables of the type '%s' can't be converted to flow variables of the type '%s'.", from, to));
    }



    @SuppressWarnings("deprecation")
    FlowVariable.Type getType() {
        return FlowVariable.Type.OTHER;
    }

    // TODO get class of simple type

    /**
     * @since 4.2
     */
    protected abstract VariableValue<T> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * @since 4.2
     */
    protected abstract VariableValue<T> newValue(final T v);


    final void save(final VariableValue<T> value, final NodeSettingsWO settings) {
        settings.addString(CFG_CLASS, getIdentifier());
        value.getType().saveValue(settings, value);
    }

    /**
     * @since 4.2
     */
    protected abstract void saveValue(final NodeSettingsWO settings, final VariableValue<T> v);

    @Override
    public String toString() {
        return getIdentifier();
    }

}
