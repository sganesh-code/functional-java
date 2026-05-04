package io.github.senthilganeshs.fj.ds;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A Super Type Token used to capture generic type information at runtime.
 * Enables safe narrowing of polymorphic structures without losing type parameters.
 * 
 * @param <T> The full generic type to capture.
 */
public abstract class TypeReference<T> {
    private final Type type;
    private final Class<T> rawType;

    @SuppressWarnings("unchecked")
    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class) {
            throw new RuntimeException("Missing type parameter for TypeReference");
        }
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        
        if (this.type instanceof Class) {
            this.rawType = (Class<T>) this.type;
        } else if (this.type instanceof ParameterizedType) {
            this.rawType = (Class<T>) ((ParameterizedType) this.type).getRawType();
        } else {
            throw new RuntimeException("Unsupported type: " + this.type);
        }
    }

    public Type getType() {
        return type;
    }

    public Class<T> getRawType() {
        return rawType;
    }

    public boolean isInstance(Object obj) {
        return rawType.isInstance(obj);
    }
}
