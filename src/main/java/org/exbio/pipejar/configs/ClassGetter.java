package org.exbio.pipejar.configs;

import java.util.List;
import java.util.Map;

public class ClassGetter {
    public static <T> Class<List<T>> getList() {
        return (Class<List<T>>) ((Class) List.class);
    }


    public static <K, V> Class<Map<K, V>> getMap() {
        return (Class<Map<K, V>>) ((Class) Map.class);
    }
}
