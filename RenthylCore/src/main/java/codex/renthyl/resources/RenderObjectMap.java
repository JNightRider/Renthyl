/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.resources;

import codex.renthyl.FGPipelineContext;
import codex.renthyl.modules.ModuleIndex;
import codex.renthyl.definitions.ResourceDef;
import com.jme3.math.FastMath;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages creation, reallocation, and disposal of {@link RenderObject RenderObjects}
 * globally across all {@link codex.renthyl.FrameGraph FrameGraphs}.
 * 
 * @author codex
 */
public class RenderObjectMap {
    
    private final FGPipelineContext context;
    private final Map<Long, RenderObject> objectMap = new ConcurrentHashMap<>();
    private int staticTimeout = 1;
    
    /**
     * 
     * @param context
     */
    public RenderObjectMap(FGPipelineContext context) {
        this.context = context;
    }
    
    private <T> RenderObject<T> create(ResourceDef<T> def) {
        return create(def, def.createResource());
    }
    private <T> RenderObject<T> create(ResourceDef<T> def, T value) {
        if (value == null) {
            throw new NullPointerException("Object created by definition cannot be null.");
        }
        RenderObject obj = new RenderObject(def, value, staticTimeout);
        objectMap.put(obj.getId(), obj);
        return obj;
    }
    private <T> void allocate(ResourceView<T> resource, EvaluatedResource eval) {
        resource.setObject(eval.getResource(), resource.getDefinition().applyResource(eval.getResource().getObject()));
    }
    private boolean isAvailable(RenderObject object) {
        return !object.isAcquired() && !object.isConstant();
    }
    
    /**
     * Allocates a render object to the ResourceView.
     * <p>
     * First, if this resource holds an object id, then corresponding render object,
     * if it still exists, will be tried for reallocation. If that fails, each render object
     * will be tried for reallocation. Finally, if that fails, a new render object
     * will be created and allocated to the resource.
     * <p>
     * Each tried render object is tried for direct and indirect allocation. The exact
     * distinction between these two are defined by the resource's definition, but
     * direct allocation are always preferred over indirect allocations. Thus, even
     * if a resource qualifies for indirect allocation, this method will continue
     * trying objects until a direct allocation is found, or all objects have been
     * tried.
     * 
     * @param <T>
     * @param resource 
     * @param async true to execute asynchronous methods, otherwise non-threadsafe methods will
     * be used in the interest of efficiency
     */
    public <T> void allocate(ResourceView<T> resource, boolean async) {
        if (async) {
            allocateAsync(resource);
        } else {
            allocateSync(resource);
        }
    }
    /**
     * Allocates the specific render object that is associated with the
     * resource view.
     * 
     * @param <T>
     * @param resource resource to allocate to
     * @param async true to run asynchronous threadsafe operations
     * @param ignoreReservations if true, reservations on the object will be ignored
     * @return true if the resource was allocated to
     */
    public <T> boolean allocateSpecific(ResourceView<T> resource, boolean async, boolean ignoreReservations) {
        if (resource.isUndefined()) {
            throw new IllegalArgumentException("Cannot allocate object to an undefined resource.");
        }
        ResourceDef<T> def = resource.getDefinition();
        if (def == null) {
            throw new NullPointerException("Resource definition cannot be null in this context.");
        }
        if (def.isUseExisting()) {
            EvaluatedResource eval = new EvaluatedResource();
            if (async) {
                return allocateSpecificAsync(resource, eval, ignoreReservations);
            } else {
                allocateSpecificSync(resource, eval, ignoreReservations);
                if (eval.isFinal()) {
                    allocate(resource, eval);
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Allocates a render object from the object cache.
     * <p>
     * If the resource view's definition does not approve the object selected
     * to allocate, an exception will be thrown. This is notably different from normal
     * allocation, which creates a new object if the definition does not approve
     * any tried objects.
     * 
     * @param <T>
     * @param cache
     * @param resource
     * @param key
     * @return true if allocation successful
     */
    public <T> boolean allocateFromCache(ResourceCache cache, ResourceView<T> resource, String key) {
        RenderObject obj = cache.fetch(key);
        if (obj == null) {
            return false;
        }
        EvaluatedResource eval = new EvaluatedResource();
        ResourceDef<T> def = resource.getDefinition();
        eval.add(obj, def.evaluateResource(obj.getObject()));
        if (!eval.isNull()) {
            throw new NullPointerException("Allocation from cache denied by resource definition.");
        }
        allocate(resource, eval);
        objectMap.put(obj.getId(), obj);
        return true;
    }
    
    private <T> void allocateSync(ResourceView<T> resource) {
        if (resource.isUndefined()) {
            throw new IllegalArgumentException("Cannot allocate object to an undefined resource.");
        }
        ResourceDef<T> def = resource.getDefinition();
        if (def == null) {
            throw new NullPointerException("Resource definition cannot be null in this context.");
        }
        if (def.isUseExisting()) {
            EvaluatedResource eval = new EvaluatedResource();
            // first try allocating a specific object, which is much faster
            allocateSpecificSync(resource, eval, false);
            if (eval.isFinal()) {
                allocate(resource, eval);
                return;
            }
            // find object to allocate
            for (RenderObject obj : objectMap.values()) {
                if (isAvailable(obj) && obj.isAllowCasualAllocation() && !obj.isReservedWithin(resource.getLifeTime())) {
                    // try applying a direct resource
                    eval.add(obj, def.evaluateResource(obj.getObject()));
                    if (eval.isFinal()) {
                        allocate(resource, eval);
                        return;
                    }
                }
            }
            if (!eval.isNull()) {
                allocate(resource, eval);
                return;
            }
        }
        // create new object
        resource.setObject(create(def));
    }
    private <T> boolean allocateSpecificSync(ResourceView<T> resource, EvaluatedResource eval, boolean ignoreReservations) {
        ResourceDef<T> def = resource.getDefinition();
        long id = resource.getObjectId();
        if (id < 0) return false;
        // allocate reserved object
        RenderObject obj = objectMap.get(id);
        if (obj != null && isAvailable(obj) && (obj.claimReservation(resource.getProducer().getIndex())
                || ignoreReservations || !obj.isReservedWithin(resource.getLifeTime()))
                && eval.add(obj, def.evaluateResource(obj.getObject())) && eval.isFinal()) {
            allocate(resource, eval);
            return true;
        }
        return false;
    }
    private <T> void allocateAsync(ResourceView<T> resource) {
        if (resource.isUndefined()) {
            throw new IllegalArgumentException("Cannot allocate object to an undefined resource.");
        }
        ResourceDef<T> def = resource.getDefinition();
        if (def.isUseExisting()) {
            final EvaluatedResource eval = new EvaluatedResource();
            // first try allocating a specific object, which is much faster
            if (allocateSpecificAsync(resource, eval, false)) {
                return;
            }
            // find object to allocate
            final LinkedList<RenderObject> skipped = new LinkedList<>();
            final Iterator<RenderObject> it = objectMap.values().iterator();
            while (it.hasNext() || !skipped.isEmpty()) {
                RenderObject obj;
                if (it.hasNext()) {
                    obj = it.next();
                } else if (FastMath.rand.nextBoolean()) {
                    obj = skipped.removeFirst();
                } else {
                    obj = skipped.removeLast();
                }
                if (isAvailable(obj) && obj.isAllowCasualAllocation()) {
                    if ((it.hasNext() || !skipped.isEmpty()) && obj.isInspect()) {
                        // Inspect this object later, because something else is inspecting it.
                        // This makes this thread try other objects first, instead of waiting
                        // for a synchronized block to be available.
                        skipped.addLast(obj);
                        continue;
                    }
                    // If multiple threads do happen to be here at the same time, ensure only one
                    // will inspect at a time.
                    synchronized (obj) {
                        // The thread we were waiting on may have claimed the object, so check again
                        // if it is available.
                        if (!isAvailable(obj)) {
                            continue;
                        }
                        obj.startInspect(); // start inspection
                        if (!obj.isReservedWithin(resource.getLifeTime())
                                && eval.add(obj, def.evaluateResource(obj.getObject())) && eval.isFinal()) {
                            allocate(resource, eval);
                            obj.endInspect(); // end inspection
                            return;
                        }
                        obj.endInspect(); // end inspection
                    }
                }
            }
            // allocate indirect object
            if (!eval.isNull()) synchronized (eval.getResource()) {
                // check again if object is available
                if (isAvailable(eval.getResource())) {
                    allocate(resource, eval);
                    return;
                }
            }
        }
        // create new object
        resource.setObject(create(def));
    }
    private <T> boolean allocateSpecificAsync(ResourceView<T> resource, EvaluatedResource eval, boolean ignoreReservations) {
        ResourceDef<T> def = resource.getDefinition();
        long id = resource.getObjectId();
        if (id < 0) return false;
        // allocate reserved object
        RenderObject obj = objectMap.get(id);        
        if (obj != null && isAvailable(obj)) synchronized (obj) {
            obj.startInspect(); // start inspection
            if ((obj.claimReservation(resource.getProducer().getIndex())
                    || ignoreReservations || !obj.isReservedWithin(resource.getLifeTime()))
                    && eval.add(obj, def.evaluateResource(obj.getObject())) && eval.isFinal()) {
                allocate(resource, eval);
                obj.endInspect(); // end inspection
                return true;
            }
            obj.endInspect(); // end inspection
        }
        return false;
    }
    
    /**
     * Makes a reservation of render object holding the specified id at the render
     * pass index.
     * <p>
     * A reservation blocks other reallocation requests for the remainder of the frame.
     * It is not strictly guaranteed to block all other requests, so it is not considered
     * good practice to rely on a reservation blocking all such requests.
     * 
     * @param objectId id of the object to reserve
     * @param index index to reserve the object at
     * @return true if the referenced object exists
     */
    public boolean reserve(long objectId, ModuleIndex index) {
        RenderObject obj = objectMap.get(objectId);
        if (obj != null) {
            obj.reserve(index);
            return true;
        }
        return false;
    }
    /**
     * Disposes the render object pointed to by the ResourceView's internal ticket.
     * 
     * @param resource 
     */
    public void dispose(ResourceView resource) {
        long id = resource.getObjectId();
        if (id >= 0) {
            RenderObject obj = objectMap.remove(id);
            if (obj != null) {
                obj.dispose();
            }
        }
    }
    /**
     * Caches the object at the key.
     * 
     * @param cache
     * @param objectId 
     * @param key 
     * @return  
     */
    public boolean cache(ResourceCache cache, long objectId, String key) {
        RenderObject obj = objectMap.remove(objectId);
        if (obj != null) {
            cache.add(key, obj);
            // the object can no longer be reserved, so clear reservations now
            obj.clearReservations();
            return true;
        }
        return false;
    }

    /**
     * Clears reservations of all tracked render objects.
     */
    public void clearReservations() {
        for (RenderObject obj : objectMap.values()) {
            obj.clearReservations();
        }
    }
    /**
     * Flushes the map.
     * <p>
     * Any render objects that have not been used for a number of frames are disposed.
     */
    public void flushMap() {
        flushCollection(objectMap.values());
    }
    /**
     * Flushes the given object cache.
     * 
     * @param cache 
     */
    public void flushCache(ResourceCache cache) {
        flushCollection(cache.values());
    }
    /**
     * Clears the map and cache.
     * <p>
     * All tracked render objects are disposed.
     */
    public void clearMap() {
        disposeCollection(objectMap.values());
        objectMap.clear();
    }
    
    private void flushCollection(Iterable<RenderObject> iterable) {
        for (Iterator<RenderObject> it = iterable.iterator(); it.hasNext();) {
            RenderObject obj = it.next();
            if (obj.isAcquired()) {
                throw new IllegalStateException(obj + " is not released.");
            }
            if (!obj.tickTimeout()) {
                obj.dispose();
                it.remove();
                continue;
            }
            obj.setConstant(false);
        }
    }
    private void disposeCollection(Iterable<RenderObject> iterable) {
        for (RenderObject obj : iterable) {
            obj.dispose();
        }
    }
    
    /**
     * Sets the default number of frame boundaries an object can experience without
     * being used before being disposed.
     * <p>
     * default=1 (can survive one frame boundary)
     * 
     * @param staticTimeout 
     */
    public void setStaticTimeout(int staticTimeout) {
        this.staticTimeout = staticTimeout;
    }
    
    /**
     * Gets the default number of frame boundaries an object can experience without
     * being used before being disposed.
     * 
     * @return 
     */
    public int getStaticTimeout() {
        return staticTimeout;
    }
    
}
