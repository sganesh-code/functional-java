package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional HashMap implemented using a Hash Array Mapped Trie (HAMT).
 * Provides near O(1) performance for lookups and updates.
 */
public interface HashMap<K, V> extends Collection<HashMap.Entry<K, V>> {

    record Entry<K, V>(K key, V value) {}

    Maybe<V> get(K key);

    HashMap<K, V> put(K key, V value);

    HashMap<K, V> remove(K key);

    static <K, V> HashMap<K, V> nil() {
        return new HAMT<>(null, 0);
    }

    final class HAMT<K, V> implements HashMap<K, V> {
        private final Node root;
        private final int size;

        HAMT(Node root, int size) {
            this.root = root;
            this.size = size;
        }

        @Override
        public Maybe<V> get(K key) {
            if (root == null) return Maybe.nothing();
            return root.get(0, key.hashCode(), key);
        }

        @Override
        public HashMap<K, V> put(K key, V value) {
            Node newRoot = (root == null) 
                ? new LeafNode(key.hashCode(), key, value)
                : root.put(0, key.hashCode(), key, value);
            
            // This size calculation is a bit naive if the key already exists
            // We can check if the key already exists using map().fromMaybe()
            int newSize = size + ((Maybe<Integer>) get(key).map(v -> 0)).fromMaybe(1);
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
            return (Collection<R>) HashMap.nil();
        }

        @Override
        public Collection<Entry<K, V>> build(Entry<K, V> input) {
            return put(input.key, input.value);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn) {
            if (root == null) return seed;
            return root.foldl(seed, fn);
        }

        @Override
        public String toString() {
            return foldl("{", (r, e) -> r + (r.equals("{") ? "" : ",") + e.key + "->" + e.value) + "}";
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

        @Override
        public <K, V> Maybe<V> get(int shift, int hash, K key) {
            if (this.hash == hash && this.key.equals(key)) {
                return Maybe.some((V) value);
            }
            return Maybe.nothing();
        }

        @Override
        public <K, V> Node put(int shift, int hash, K key, V value) {
            if (this.hash == hash && this.key.equals(key)) {
                return new LeafNode(hash, key, value);
            }
            // Collision or deeper level
            return IndexedNode.fromLeaf(shift, this).put(shift, hash, key, value);
        }

        @Override
        public <K, V> Node remove(int shift, int hash, K key) {
            if (this.hash == hash && this.key.equals(key)) return null;
            return this;
        }

        @Override
        public <K, V, R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn) {
            return fn.apply(seed, new Entry<>((K) key, (V) value));
        }
    }

    final class IndexedNode implements Node {
        private final int bitmap;
        private final Node[] children;

        IndexedNode(int bitmap, Node[] children) {
            this.bitmap = bitmap;
            this.children = children;
        }

        static IndexedNode fromLeaf(int shift, LeafNode leaf) {
            int bit = 1 << ((leaf.hash >>> shift) & 0x1f);
            return new IndexedNode(bit, new Node[]{leaf});
        }

        @Override
        public <K, V> Maybe<V> get(int shift, int hash, K key) {
            int bit = 1 << ((hash >>> shift) & 0x1f);
            if ((bitmap & bit) == 0) return Maybe.nothing();
            int index = Integer.bitCount(bitmap & (bit - 1));
            return children[index].get(shift + 5, hash, key);
        }

        @Override
        public <K, V> Node put(int shift, int hash, K key, V value) {
            int bit = 1 << ((hash >>> shift) & 0x1f);
            int index = Integer.bitCount(bitmap & (bit - 1));
            if ((bitmap & bit) != 0) {
                Node newChild = children[index].put(shift + 5, hash, key, value);
                Node[] newChildren = children.clone();
                newChildren[index] = newChild;
                return new IndexedNode(bitmap, newChildren);
            } else {
                Node[] newChildren = new Node[children.length + 1];
                System.arraycopy(children, 0, newChildren, 0, index);
                newChildren[index] = new LeafNode(hash, key, value);
                System.arraycopy(children, index, newChildren, index + 1, children.length - index);
                return new IndexedNode(bitmap | bit, newChildren);
            }
        }

        @Override
        public <K, V> Node remove(int shift, int hash, K key) {
            int bit = 1 << ((hash >>> shift) & 0x1f);
            if ((bitmap & bit) == 0) return this;
            int index = Integer.bitCount(bitmap & (bit - 1));
            Node newChild = children[index].remove(shift + 5, hash, key);
            if (newChild == children[index]) return this;
            if (newChild == null) {
                if (children.length == 1) return null;
                Node[] newChildren = new Node[children.length - 1];
                System.arraycopy(children, 0, newChildren, 0, index);
                System.arraycopy(children, index + 1, newChildren, index, children.length - index - 1);
                return new IndexedNode(bitmap ^ bit, newChildren);
            }
            Node[] newChildren = children.clone();
            newChildren[index] = newChild;
            return new IndexedNode(bitmap, newChildren);
        }

        @Override
        public <K, V, R> R foldl(R seed, BiFunction<R, Entry<K, V>, R> fn) {
            R res = seed;
            for (Node child : children) {
                res = child.foldl(res, fn);
            }
            return res;
        }
    }
}
