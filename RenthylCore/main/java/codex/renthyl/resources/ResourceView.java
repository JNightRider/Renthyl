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

import codex.renthyl.modules.ModuleIndex;
import codex.renthyl.definitions.ResourceDef;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an existing or future resource used for rendering.
 * <p>
 * Unlike {@link RenderObject}, this is only loosely connected to a raw
 * rendering resource. In fact, the creation of a ResourceView does not
 * necessarily predict the creation of a raw resource, due to culling.
 * 
 * @author codex
 * @param <T>
 */
public class ResourceView <T> {
    
    private final ResourceUser producer;
    private final ResourceDef<T> def;
    private final ResourceTicket<T> ticket;
    private final TimeFrame lifetime;
    private final LinkedList<ResourceView> dependencies = new LinkedList<>();
    private final AtomicInteger refs = new AtomicInteger(0);
    private final AtomicBoolean released = new AtomicBoolean(false);
    private RenderObject object;
    private T resource;
    private boolean temporary = false;
    private boolean undefined = false;
    
    /**
     * 
     * @param producer
     * @param def
     * @param declaringTicket 
     */
    public ResourceView(ResourceUser producer, ResourceDef<T> def, ResourceTicket<T> declaringTicket) {
        this.producer = producer;
        this.def = def;
        this.ticket = new ResourceTicket<>(declaringTicket.getName());
        this.lifetime = new TimeFrame(this.producer.getIndex(), 0);
        declaringTicket.setBindFlag();
    }
    
    /**
     * Reference this resource from the specified renderpass index
     * using the ticket.
     * 
     * @param index 
     * @param ticket 
     */
    public void reference(ModuleIndex index, ResourceTicket ticket) {
        if (isTemporary()) {
            throw new IllegalStateException("Temporary resource cannot be referenced.");
        }
        lifetime.extendTo(index);
        refs.addAndGet(1);
        ticket.setBindFlag();
    }
    /**
     * Releases this resource from one user.
     * 
     * @param list
     * @param ticket
     * @return true if this resource is used after the release
     */
    public boolean release(ResourceList list, ResourceTicket ticket) {
        boolean complete = refs.addAndGet(-1) < 0;
        if (complete) {
            setObject(null);
        }
        released.set(true);
        ticket.clearBindFlag();
        return !complete;
    }
    /**
     * Releases this resource from a culled user.
     * <p>
     * This does not affect resource's using this resource as a dependency.
     * 
     * @return true if this resource is still used after the release by
     * a user other than the declaring user
     */
    public boolean cull() {
        return refs.addAndGet(-1) > 0;
    }
    /**
     * Claims the resource for reading.
     * 
     * @return 
     */
    public boolean claimReadPermissions() {
        return ((def == null || def.isReadConcurrent()) && released.get()) || released.getAndSet(false);
    }
    
    /**
     * Registers a ResourceView to merge into this ResourceView.
     * 
     * @param view 
     */
    public void registerDependency(ResourceView view) {
        dependencies.add(view);
    }
    /**
     * Applies registered merges.
     */
    public void applyDependencies() {
        for (Iterator<ResourceView> it = dependencies.iterator(); it.hasNext();) {
            ResourceView m = it.next();
            if (m.isReferenced()) {
                //lifetime.merge(m.lifetime);
                m.lifetime.merge(lifetime);
                m.refs.addAndGet(1);
            } else {
                it.remove();
            }
        }
    }
    
    /**
     * Sets the render object held by this resource.
     * 
     * @param object 
     */
    public void setObject(RenderObject<T> object) {
        if (object != null) {
            setObject(object, object.getObject());
        } else {
            if (this.object != null) {
                this.object.release();
            }
            this.object = null;
            resource = null;
        }
    }
    /**
     * Sets the render object and concrete resource held by this render resource.
     * 
     * @param object
     * @param resource 
     */
    public void setObject(RenderObject object, T resource) {
        Objects.requireNonNull(object, "Object cannot be null.");
        Objects.requireNonNull(resource, "Object resource cannot be null.");
        if (undefined) {
            throw new IllegalStateException("Resource is already undefined.");
        }
        if (object.isAcquired()) {
            throw new IllegalStateException("Object is already acquired.");
        }
        this.object = object;
        this.resource = resource;
        this.object.acquire();
        ticket.setObjectId(this.object.getId());
    }
    /**
     * Directly sets the raw resource held by this render resource.
     * 
     * @param resource 
     */
    public void setPrimitive(T resource) {
        if (!isPrimitive()) {
            throw new IllegalStateException("Resource unexpectedly assigned primitively.");
        }
        if (undefined) {
            throw new IllegalStateException("Resource is already marked as undefined.");
        }
        object = null;
        this.resource = resource;
    }
    /**
     * Marks this resource as undefined.
     */
    public void setUndefined() {
        if (resource != null) {
            throw new IllegalStateException("Resource is already defined.");
        }
        undefined = true;
    }
    /**
     * Returns true if this resource always survives cull by reference.
     * 
     * @param temporary 
     */
    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }
    
    /**
     * Gets this resource's producer.
     * 
     * @return 
     */
    public ResourceUser getProducer() {
        return producer;
    }
    /**
     * Gets the resource definition.
     * 
     * @return 
     */
    public ResourceDef<T> getDefinition() {
        return def;
    }
    /**
     * Gets the resource ticket.
     * 
     * @return 
     */
    public ResourceTicket<T> getTicket() {
        return ticket;
    }
    /**
     * Gets the lifetime of this resource in render pass indices.
     * 
     * @return 
     */
    public TimeFrame getLifeTime() {
        return lifetime;
    }
    /**
     * Gets the render object.
     * 
     * @return 
     */
    public RenderObject getObject() {
        return object;
    }
    /**
     * Gets the list of resources this resource depends on.
     * 
     * @return 
     */
    public LinkedList<ResourceView> getDependencies() {
        return dependencies;
    }
    /**
     * Gets the concrete resource.
     * 
     * @return 
     */
    public T getResource() {
        return resource;
    }
    /**
     * Gets the index of this resource.
     * 
     * @return 
     */
    public int getIndex() {
        return ticket.getWorldIndex();
    }
    /**
     * Gets the number of references to this resource, not including
     * the reference from the producer.
     * <p>
     * Zero references means that no users are referencing this resource
     * except the producing user. -1 references indicates that no
     * users are referencing this resource with no exceptions.
     * 
     * @return 
     */
    public int getNumReferences() {
        return refs.get();
    }
    
    /**
     * Returns true if this resource is virtual.
     * <p>
     * A resource is virtual when it does not hold a concrete resource
     * and is not set as undefined.
     * 
     * @return 
     */
    public boolean isVirtual() {
        return resource == null && !undefined;
    }
    /**
     * Returns true if this resource is primitive.
     * <p>
     * A resource is primitive when it holds a concrete resource without a
     * corresponding render object. Primitive resources are handled niavely,
     * because they are not directly associated with a render object.
     * 
     * @return 
     */
    public boolean isPrimitive() {
        return def == null;
    }
    /**
     * Returns true if this resource is referenced by more than one user.
     * 
     * @return 
     */
    public boolean isReferenced() {
        return refs.get() > 0;
    }
    /**
     * Returns true if this resource not referenced by any users.
     * 
     * @return 
     */
    public boolean isFullyReleased() {
        return refs.get() < 0;
    }
    /**
     * Returns true if this resource is referenced by exactly one user.
     * 
     * @return 
     */
    public boolean isPartiallyReleased() {
        return refs.get() == 0;
    }
    /**
     * Returns true if this resource is used (including the producer).
     * 
     * @return 
     */
    public boolean isUsed() {
        return refs.get() >= 0;
    }
    /**
     * Returns true if this resource is marked as undefined.
     * 
     * @return 
     */
    public boolean isUndefined() {
        return undefined;
    }
    /**
     * 
     * @return 
     */
    public boolean isTemporary() {
        return temporary;
    }
    /**
     * Return true if this resource is available for reading.
     * <p>
     * This is true typically after the first release occurs.
     * 
     * @return 
     */
    public boolean isReadAvailable() {
        return released.get();
    }
    /**
     * Returns true if this ResoureView is dependent on one or
     * more other ResourceViews.
     * 
     * @return 
     */
    public boolean isDependent() {
        return !dependencies.isEmpty();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "["+producer+", "+ticket+"]";
    }
    
}
