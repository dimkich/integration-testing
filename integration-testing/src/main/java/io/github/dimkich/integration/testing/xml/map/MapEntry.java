package io.github.dimkich.integration.testing.xml.map;

public interface MapEntry<K, V> {
    K getKey();

    void setKey(K key);

    V getValue();

    void setValue(V value);
}
