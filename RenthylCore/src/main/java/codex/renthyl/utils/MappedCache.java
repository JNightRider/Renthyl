/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.utils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Maps a add of elements by a key type, which can be removed by lack of use.
 * 
 * @author codex
 * @param <T>
 * @param <K>
 */
public abstract class MappedCache <K, T> {
    
    private final HashMap<K, CachedElement<T>> cache = new HashMap<>();
    private K localKey;
    private int staticTimeout;

    public MappedCache() {
        this(null, 2);
    }
    public MappedCache(K localKey) {
        this(localKey, 1);
    }
    public MappedCache(int staticTimeout) {
        this(null, staticTimeout);
    }
    public MappedCache(K localKey, int staticTimeout) {
        this.localKey = localKey;
        this.staticTimeout = staticTimeout;
    }
    
    /**
     * Creates an element for the key when no element already
     * exists in the cache for the key.
     * 
     * @param key
     * @return newly created element
     */
    protected abstract T createElement(K key);
    /**
     * Destroys the element.
     * 
     * @param element 
     */
    protected abstract void destroyElement(T element);
    
    /**
     * Fetch an element by the locally stored key.
     * 
     * @return 
     */
    public T fetch() {
        return fetch(null);
    }
    /**
     * Fetches an element using the given key.
     * 
     * @param key
     * @return 
     */
    public T fetch(K key) {
        if (key == null) {
            key = localKey;
        }
        if (key == null) {
            throw new NullPointerException("Cache local key not add.");
        }
        CachedElement<T> el = cache.get(key);
        if (el == null) {
            el = new CachedElement<>(createElement(key), staticTimeout);
            cache.put(key, el);
        }
        el.refresh();
        return el.object;
    }
    
    /**
     * Flushes the cache.
     * <p>
     * Any elements that have not been used in the specified {@link #getStaticTimeout() timeout}
     * number of flushes will be removed.
     */
    public void flush() {
        for (Iterator<CachedElement<T>> it = cache.values().iterator(); it.hasNext();) {
            CachedElement<T> el = it.next();
            if (!el.tick()) {
                destroyElement(el.object);
                it.remove();
            }
        }
    }
    /**
     * Clears the cache, removing all elements.
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Sets the local key used if none is specified.
     * 
     * @param localKey 
     */
    public void setLocalKey(K localKey) {
        this.localKey = localKey;
    }
    /**
     * Sets the number of flushes an element can survive since its last use.
     * 
     * @param staticTimeout 
     */
    public void setStaticTimeout(int staticTimeout) {
        this.staticTimeout = staticTimeout;
    }
    
    /**
     * 
     * @return 
     * @see #setLocalKey(java.lang.Object) 
     */
    public K getLocalKey() {
        return localKey;
    }
    /**
     * 
     * @return 
     * @see #setStaticTimeout(int) 
     */
    public int getStaticTimeout() {
        return staticTimeout;
    }
    /**
     * 
     * @return 
     */
    public int size() {
        return cache.size();
    }
    
    private static class CachedElement <T> {
        
        public final T object;
        public final int refresh;
        public int life;

        public CachedElement(T object, int life) {
            this.object = object;
            this.refresh = this.life = life;
        }
        
        public void refresh() {
            life = refresh;
        }
        
        public boolean tick() {
            return life-- > 0;
        }
        
    }
    
}
