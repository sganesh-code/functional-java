package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Maybe;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

/**
 * Automated Optics generation for Java Records using Method References.
 */
public final class RecordOptics {

    private RecordOptics() {}

    /**
     * Automatically creates a Lens for a Record component using a method reference.
     * 
     * @param <S> The Record type (Source)
     * @param <A> The Component type (Attribute)
     * @param recordClass The class of the record
     * @param getter A method reference to the record component (e.g., User::name)
     * @return A Lens focusing on that component
     */
    @SuppressWarnings("unchecked")
    public static <S, A> Lens<S, A> of(Class<S> recordClass, RecordComponentFunction<S, A> getter) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException(recordClass.getName() + " is not a Record");
        }

        try {
            // 1. Extract the method name from the serialized lambda
            Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(getter);
            String methodName = lambda.getImplMethodName();

            // 2. Find the RecordComponent matching the method name
            RecordComponent[] components = recordClass.getRecordComponents();
            int index = -1;
            for (int i = 0; i < components.length; i++) {
                if (components[i].getName().equals(methodName)) {
                    index = i;
                    break;
                }
            }

            if (index == -1) {
                throw new NoSuchMethodException("No record component found for method: " + methodName);
            }

            // 3. Find the Canonical Constructor
            Class<?>[] paramTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
            Constructor<S> constructor = recordClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);

            final int targetIndex = index;

            // 4. Build the Lens
            return Lens.of(getter, (newValue, source) -> {
                try {
                    Object[] values = new Object[components.length];
                    for (int i = 0; i < components.length; i++) {
                        Method accessor = components[i].getAccessor();
                        accessor.setAccessible(true);
                        values[i] = accessor.invoke(source);
                    }
                    values[targetIndex] = newValue;
                    return constructor.newInstance(values);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to update record component", e);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Lens for record: " + recordClass.getName(), e);
        }
    }
}
