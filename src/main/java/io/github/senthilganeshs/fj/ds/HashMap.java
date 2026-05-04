package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A persistent Hash Array Mapped Trie (HAMT) implementation.
 * 
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public interface HashMap<K, V> extends Collection<HashMap.Entry<K, V>> {

    int BITS = 5;
    int WIDTH = 1 << BITS;
    int MASK = WIDTH - 1;

    record Entry<K, V>(K key, V value) {}

    Maybe<V> get(K key);

    /**
     * Converts this persistent map to a standard mutable java.util.Map.
     */
    default java.util.Map<K, V> toJavaMap() {
        java.util.Map<K, V> map = new java.util.HashMap<>();
        this.forEach(e -> map.put(e.key(), e.value()));
        return map;
    }

    HashMap<K, V> put(K key, V value);

    HashMap<K, V> remove(K key);

    int size();

    default Collection<K> keys() {
        return map(Entry::key);
    }

    default Collection<V> values() {
        return map(Entry::value);
    }

    default io.github.senthilganeshs.fj.optic.AffineTraversal<HashMap<K, V>, V> at(K key) {
        return io.github.senthilganeshs.fj.optic.AffineTraversal.of(
            m -> m.get(key),
            (v, m) -> m.put(key, v)
        );
    }

    @SuppressWarnings("unchecked")
    static <K, V> HashMap<K, V> nil() {
        return (HashMap<K, V>) HAMT.EMPTY;
    }

    final class HAMT<K, V> implements HashMap<K, V> {
        static final HashMap<?, ?> EMPTY = new HAMT<>(null, 0);

        private final Node root;
        private final int size;

        HAMT(Node root, int size) {
            this.root = root;
            this.size = size;
        }

        @Override
        public int size() {
            return size;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Maybe<V> get(K key) {
            if (root == null) return Maybe.nothing();
            return root.get(0, key.hashCode(), key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public HashMap<K, V> put(K key, V value) {
            Node newRoot = (root == null) 
                ? new LeafNode(key.hashCode(), key, value)
                : root.put(0, key.hashCode(), key, value);
            
            int newSize = size + (get(key).map(v -> 0)).orElse(1);
            return new HAMT<>(newRoot, newSize);
        }

        @Override
        public HashMap<K, V> remove(K key) {
            if (root == null) return this;
            Node newRoot = root.remove(0, key.hashCode(), key);
            if (newRoot == root) return this;
            return new HAMT<>(newRoot, size - 1);
        }

        @Override
        public <R> Collection<R> empty() {
            return List.nil();
        }

        @Override
        public Collection<Entry<K, V>> build(Entry<K, V> input) {
            return put(input.key, input.value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn) {
            if (root == null) return seed;
            return root.foldl(seed, fn);
        }

        @Override
        public String toString() {
            return foldl("{", (r, e) -> r + (r.equals("{") ? "" : ",") + e.key + "->" + e.value) + "}";
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof HashMap)) return false;
            HashMap<K, V> o = (HashMap<K, V>) other;
            if (o.size() != size) return false;
            return foldl(true, (acc, entry) -> acc && o.get(entry.key()).map(v -> v.equals(entry.value())).orElse(false));
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }

    interface Node {
        <K, V> Maybe<V> get(int shift, int hash, K key);
        <K, V> Node put(int shift, int hash, K key, V value);
        <K, V> Node remove(int shift, int hash, K key);
        <K, V, R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn);
    }

    final class LeafNode implements Node {
        final int hash;
        final Object key;
        final Object value;

        LeafNode(int hash, Object key, Object value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K, V> Maybe<V> get(int shift, int hash, K key) {
            if (this.hash == hash && this.key.equals(key)) {
                return Maybe.some((V) value);
            }
            return Maybe.nothing();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K, V> Node put(int shift, int hash, K key, V value) {
            if (this.hash == hash) {
                if (this.key.equals(key)) {
                    return new LeafNode(hash, key, value);
                } else {
                    return new CollisionNode(hash, List.of(new Entry<>((K)this.key, (V)this.value), new Entry<>(key, value)));
                }
            }
            return IndexedNode.fromLeaf(shift, this).put(shift, hash, key, value);
        }

        @Override
        public <K, V> Node remove(int shift, int hash, K key) {
            if (this.hash == hash && this.key.equals(key)) return null;
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K, V, R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn) {
            return fn.apply(seed, new Entry<>((K) key, (V) value));
        }
    }

    final class CollisionNode implements Node {
        final int hash;
        final List<Entry<?, ?>> entries;

        CollisionNode(int hash, List<Entry<?, ?>> entries) {
            this.hash = hash;
            this.entries = entries;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K, V> Maybe<V> get(int shift, int hash, K key) {
            if (this.hash != hash) return Maybe.nothing();
            return (Maybe<V>) (Maybe<?>) entries.filter(e -> e.key().equals(key)).map(Entry::value).find(v -> true);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K, V> Node put(int shift, int hash, K key, V value) {
            if (this.hash == hash) {
                List<Entry<?, ?>> filtered = List.from(entries.filter(e -> !e.key().equals(key)));
                return new CollisionNode(hash, List.from(filtered.build(new Entry<>(key, value))));
            }
            return IndexedNode.fromNode(shift, this).put(shift, hash, key, value);
        }

        @Override
        public <K, V> Node remove(int shift, int hash, K key) {
            if (this.hash != hash) return this;
            List<Entry<?, ?>> filtered = List.from(entries.filter(e -> !e.key().equals(key)));
            if (filtered.isEmpty()) return null;
            if (filtered.length() == 1) {
                Entry<?, ?> e = filtered.head().orElse(null);
                return new LeafNode(hash, e.key(), e.value());
            }
            return new CollisionNode(hash, filtered);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K, V, R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn) {
            return entries.foldl(seed, (acc, e) -> fn.apply(acc, (Entry<K, V>) e));
        }
    }

    final class IndexedNode implements Node {
        private final int bitmap;
        private final Node[] nodes;

        IndexedNode(int bitmap, Node[] nodes) {
            this.bitmap = bitmap;
            this.nodes = nodes;
        }

        static IndexedNode fromLeaf(int shift, LeafNode leaf) {
            int hash = leaf.hash;
            int bit = 1 << ((hash >> shift) & MASK);
            return new IndexedNode(bit, new Node[]{leaf});
        }

        static IndexedNode fromNode(int shift, Node node) {
            int hash = (node instanceof CollisionNode) ? ((CollisionNode)node).hash : ((LeafNode)node).hash;
            int bit = 1 << ((hash >> shift) & MASK);
            return new IndexedNode(bit, new Node[]{node});
        }

        @Override
        public <K, V> Maybe<V> get(int shift, int hash, K key) {
            int bit = 1 << ((hash >> shift) & MASK);
            if ((bitmap & bit) == 0) return Maybe.nothing();
            int index = Integer.bitCount(bitmap & (bit - 1));
            return nodes[index].get(shift + BITS, hash, key);
        }

        @Override
        public <K, V> Node put(int shift, int hash, K key, V value) {
            int bit = 1 << ((hash >> shift) & MASK);
            int index = Integer.bitCount(bitmap & (bit - 1));
            if ((bitmap & bit) != 0) {
                Node child = nodes[index];
                Node newChild = child.put(shift + BITS, hash, key, value);
                if (newChild == child) return this;
                Node[] newNodes = nodes.clone();
                newNodes[index] = newChild;
                return new IndexedNode(bitmap, newNodes);
            } else {
                Node[] newNodes = new Node[nodes.length + 1];
                System.arraycopy(nodes, 0, newNodes, 0, index);
                newNodes[index] = new LeafNode(hash, key, value);
                System.arraycopy(nodes, index, newNodes, index + 1, nodes.length - index);
                return new IndexedNode(bitmap | bit, newNodes);
            }
        }

        @Override
        public <K, V> Node remove(int shift, int hash, K key) {
            int bit = 1 << ((hash >> shift) & MASK);
            if ((bitmap & bit) == 0) return this;
            int index = Integer.bitCount(bitmap & (bit - 1));
            Node child = nodes[index];
            Node newChild = child.remove(shift + BITS, hash, key);
            if (newChild == child) return this;
            if (newChild == null) {
                if (nodes.length == 1) return null;
                Node[] newNodes = new Node[nodes.length - 1];
                System.arraycopy(nodes, 0, newNodes, 0, index);
                System.arraycopy(nodes, index + 1, newNodes, index, nodes.length - index - 1);
                return new IndexedNode(bitmap & ~bit, newNodes);
            }
            Node[] newNodes = nodes.clone();
            newNodes[index] = newChild;
            return new IndexedNode(bitmap, newNodes);
        }

        @Override
        public <K, V, R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn) {
            R res = seed;
            for (Node node : nodes) {
                res = node.foldl(res, fn);
            }
            return res;
        }
    }
}
