package io.github.senthilganeshs.fj.ds;

public interface Map<K extends Comparable<K>, V> {

    record Entry<K extends Comparable<K>, V>(K key, V value) implements Comparable<Entry<K, V>> {
        @Override
        public int compareTo(Entry<K, V> o) {
            return key.compareTo(o.key);
        }
    }
    Maybe<V> lookup(K key);
    Map<K, V> put(K key, V value);

    Collection<K> keys();
    Collection<V> values();
    Collection<Map.Entry<K, V>> entries();

    final class BinaryTreeMap <K extends Comparable<K>, V> implements Map<K, V> {

        private final Set<Map.Entry<K, V>> entries;

        BinaryTreeMap(Set<Map.Entry<K, V>> entries) {
            this.entries = entries;
        }

        @Override
        public Maybe<V> lookup(K key) {
            return (Maybe<V>) entries.find(e -> e.key.compareTo(key) == 0).map(e ->  e.value);
        }

        @Override
        public Map<K, V> put(K key, V value) {
            return new BinaryTreeMap<>(entries.build(new Entry<>(key, value)));
        }

        @Override
        public Collection<K> keys() {
            return entries.map(e -> e.key);
        }

        @Override
        public Collection<V> values() {
            return entries.map(e -> e.value);
        }

        @Override
        public Collection<Entry<K, V>> entries() {
            return entries;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Map)) return false;
            Map<?, ?> o = (Map<?, ?>) other;
            return entries.equals(o.entries());
        }

        @Override
        public int hashCode() {
            return entries.hashCode();
        }
    }
}
