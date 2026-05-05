package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.TypeReference;
import io.github.senthilganeshs.fj.parser.JsonValue;
import io.github.senthilganeshs.fj.parser.JsonValue.*;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.util.Arrays;

/**
 * Automated Optics generation for Java Records using Method References.
 */
public final class RecordOptics {

    private RecordOptics() {}

    /**
     * Automatically creates a Lens for a Record component using a method reference.
     */
    @SuppressWarnings("unchecked")
    public static <S, A> Lens<S, A> of(Class<S> recordClass, RecordComponentFunction<S, A> getter) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException(recordClass.getName() + " is not a Record");
        }

        try {
            Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(getter);
            String methodName = lambda.getImplMethodName();

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

            Class<?>[] paramTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
            Constructor<S> constructor = recordClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);

            final int targetIndex = index;

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

    /**
     * Automatically creates a bidirectional Isomorphism between a Type and a JsonValue.
     */
    @SuppressWarnings("unchecked")
    public static <R> Iso<R, JsonValue> jsonIso(TypeReference<R> typeRef) {
        return Iso.of(
            record -> toJson(record),
            json -> (R) fromJson(json, typeRef.getRawType(), typeRef.getType())
        );
    }

    /**
     * Automatically creates a bidirectional Isomorphism between a Record and a JsonValue.
     */
    public static <R> Iso<R, JsonValue> jsonIso(Class<R> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException(recordClass.getName() + " is not a Record");
        }

        return Iso.of(
            record -> toJson(record),
            json -> {
                if (!(json instanceof JsonObject obj)) {
                    throw new RuntimeException("Expected JsonObject for record conversion, found: " + json.getClass().getSimpleName());
                }
                RecordComponent[] components = recordClass.getRecordComponents();
                Object[] values = new Object[components.length];
                for (int i = 0; i < components.length; i++) {
                    RecordComponent comp = components[i];
                    JsonValue fieldVal = obj.fields().get(comp.getName()).orElse(new JsonNull());
                    values[i] = fromJson(fieldVal, comp.getType(), comp.getGenericType());
                }
                try {
                    Class<?>[] paramTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
                    Constructor<R> constructor = recordClass.getDeclaredConstructor(paramTypes);
                    constructor.setAccessible(true);
                    return constructor.newInstance(values);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize JSON to record: " + recordClass.getName(), e);
                }
            }
        );
    }

    /**
     * General Object to JsonValue conversion.
     * Respects FJ collections and Java Records recursively.
     */
    @SuppressWarnings("unchecked")
    private static JsonValue toJson(Object record) {
        if (record == null) return new JsonNull();
        if (record instanceof JsonValue jv) return jv;
        if (record instanceof String s) return new JsonString(s);
        if (record instanceof Number n) return new JsonNumber(n.doubleValue());
        if (record instanceof Boolean b) return new JsonBoolean(b);
        if (record instanceof Maybe<?> m) return m.map(RecordOptics::toJson).orElse(new JsonNull());
        
        if (record instanceof HashMap<?, ?> m) {
            HashMap<String, JsonValue> fields = m.foldl(HashMap.nil(), (acc, entry) ->
                acc.put(String.valueOf(entry.key()), toJson(entry.value())));
            return new JsonObject(fields);
        }

        if (record instanceof List<?> l) return new JsonArray(List.from(l.map(RecordOptics::toJson)));
        
        Class<?> clazz = record.getClass();
        if (clazz.isRecord()) {
            RecordComponent[] components = clazz.getRecordComponents();
            HashMap<String, JsonValue> fields = HashMap.nil();
            for (RecordComponent comp : components) {
                try {
                    Method accessor = comp.getAccessor();
                    accessor.setAccessible(true);
                    Object val = accessor.invoke(record);
                    fields = fields.put(comp.getName(), toJson(val));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize record component " + comp.getName(), e);
                }
            }
            return new JsonObject(fields);
        }
        
        return new JsonString(record.toString());
    }

    @SuppressWarnings("unchecked")
    private static Object fromJson(JsonValue json, Class<?> type, Type genericType) {
        if (type.isAssignableFrom(json.getClass())) return json;

        if (json instanceof JsonNull) {
            if (type == Maybe.class) return Maybe.nothing();
            if (type == List.class) return List.nil();
            if (type == HashMap.class) return HashMap.nil();
            return null;
        }

        if (type == String.class && json instanceof JsonString s) return s.value();
        if ((type == Integer.class || type == int.class) && json instanceof JsonNumber n) return (int) n.value();
        if ((type == Double.class || type == double.class) && json instanceof JsonNumber n) return n.value();
        if ((type == Boolean.class || type == boolean.class) && json instanceof JsonBoolean b) return b.value();
        
        if (type == Maybe.class && genericType instanceof ParameterizedType pt) {
            Type innerType = pt.getActualTypeArguments()[0];
            if (json instanceof JsonNull) return Maybe.nothing();
            return Maybe.some(fromJson(json, (Class<?>) (innerType instanceof ParameterizedType ? ((ParameterizedType)innerType).getRawType() : innerType), innerType));
        }

        if (type == List.class && json instanceof JsonArray arr && genericType instanceof ParameterizedType pt) {
            Type innerType = pt.getActualTypeArguments()[0];
            Class<?> innerClass = (Class<?>) (innerType instanceof ParameterizedType ? ((ParameterizedType)innerType).getRawType() : innerType);
            return List.from(arr.elements().map(jv -> fromJson(jv, innerClass, innerType)));
        }

        if (type == HashMap.class && json instanceof JsonObject obj && genericType instanceof ParameterizedType pt) {
            Type valType = pt.getActualTypeArguments()[1];
            Class<?> valClass = (Class<?>) (valType instanceof ParameterizedType ? ((ParameterizedType)valType).getRawType() : valType);
            return obj.fields().foldl(HashMap.nil(), (acc, entry) -> 
                acc.put(entry.key(), fromJson(entry.value(), valClass, valType)));
        }

        if (type.isRecord()) {
            return jsonIso((Class<Object>) type).reverseGet(json);
        }

        return null;
    }
}
