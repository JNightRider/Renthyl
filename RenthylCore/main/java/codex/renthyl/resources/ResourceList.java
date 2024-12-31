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

import codex.renthyl.FrameGraph;
import codex.renthyl.modules.ModuleIndex;
import codex.renthyl.debug.GraphEventCapture;
import codex.renthyl.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Manages {@link ResourceView ResourceViews} for a {@link FrameGraph}.
 * <p>
 * ResourceList is responsible for managing resource views, which provide an abstract
 * interface between render passes and concrete objects. Render passes are expected
 * before execution to declare resources they plan on creating during execution and
 * reference resources they plan on using during execution from other render passes.
 * Then after execution, render passes are expected to release resources they are
 * finished using. The idea is to make a given render frame as internally predictable
 * as possible so that management can adequetely schedule resource allocations.
 * <p>
 * Data held in the ResourceList is necessarily transient. ResourceViews are created
 * and destroyed per-frame to account for changes within the rendering pipeline.
 * Because of this, ResourceViews are inadequete for storing long-term objects
 * such as textures, so objects are instead globally stored in the {@link RenderObjectMap}.
 * ResourceViews essentially act as indices into this map.
 * <p>
 * This setup of a transient layer referencing a more consistent global layer provides
 * several performance advantages. Textures can be reused across frames and reduce
 * the number of expensive binding operations (especially with
 * {@link #reserve(codex.renthyl.modules.ModuleIndex, codex.renthyl.resources.ResourceTicket) reserving}), 
 * and resources can be reused across multiple FrameGraphs.
 * <p>
 * It also allows for incredibly easy multithreading. Since ResourceViews
 * naturally act as barriers between threads, users do not need to insert barriers
 * themselves, which greatly simplifies the process of creating efficient
 * frame graphs.
 * 
 * @author codex
 */
public class ResourceList {
    
    /**
     * Initial size of the resource ArrayList.
     */
    private static final int INITIAL_SIZE = 20;
    
    /**
     * Maximum time to wait in milliseconds before throwing an exception.
     */
    public static final long WAIT_TIMEOUT = 5000;
    
    private final FrameGraph frameGraph;
    private RenderObjectMap map;
    private GraphEventCapture cap;
    private final ArrayList<ResourceView> resources;
    private final LinkedList<FutureReference> futureRefs = new LinkedList<>();
    private final ResourceCache cache = new ResourceCache();
    private int nextSlot = 0;
    private int textureBinds = 0;
    
    /**
     * Stores tickets that will require unbinding at reset, but will
     * not otherwise be accessible.
     */
    private final LinkedList<ResourceTicket> endangeredTickets = new LinkedList<>();
    
    public ResourceList(FrameGraph frameGraph) {
        this(frameGraph, INITIAL_SIZE);
    }
    public ResourceList(FrameGraph frameGraph, int initialListSize) {
        this.frameGraph = frameGraph;
        this.resources = new ArrayList<>(initialListSize);
    }
    
    private <T> ResourceView<T> create(ResourceUser producer, ResourceDef<T> def, ResourceTicket ticket) {
        ResourceView res = new ResourceView<>(producer, def, ticket);
        res.getTicket().setLocalIndex(add(res));
        return res;
    }
    private <T> ResourceView<T> locate(ResourceTicket<T> ticket) {
        return locate(ticket, true);
    }
    private <T> ResourceView<T> locate(ResourceTicket<T> ticket, boolean failOnMiss) {
        if (ticket == null) {
            if (failOnMiss) {
                throw new NullPointerException("Ticket cannot be null.");
            }
            return null;
        }
        final int i = ticket.getWorldIndex();
        if (i < 0) {
            if (failOnMiss) {
                throw new NullPointerException(ticket + " does not point to any resource (negative index).");
            }
            return null;
        }
        if (i < resources.size()) {
            ResourceView<T> res = resources.get(i);
            if (res != null) {
                return res;
            }
            if (failOnMiss) {
                throw new NullPointerException(ticket+" points to null resource.");
            }
        }
        if (failOnMiss) {
            throw new IndexOutOfBoundsException(ticket+" is out of bounds for size "+resources.size());
        }
        return null;
    }
    private ResourceView fastLocate(ResourceTicket ticket) {
        return resources.get(ticket.getWorldIndex());
    }
    private int add(ResourceView res) {
        assert res != null;
        if (nextSlot >= resources.size()) {
            // add resource to end of list
            resources.add(res);
            nextSlot++;
            return resources.size()-1;
        } else {
            // Insert resource into available slot.
            // Note: storing and finding the first empty slot is not necessary
            // for the current implementation. In fact, it is mostly left over
            // from a previous implementation. I probably won't remove it because
            // this case should theoretically never occur anyway, and I may need
            // it in the future.
            int i = nextSlot;
            resources.set(i, res);
            // find next available slot
            while (++nextSlot < resources.size()) {
                if (resources.get(nextSlot) == null) {
                    break;
                }
            }
            return i;
        }
    }
    private ResourceView remove(int index) {
        ResourceView prev = resources.set(index, null);
        if (prev != null && prev.isReferenced()) {
            throw new IllegalStateException("Cannot remove "+prev+" because it is referenced.");
        }
        nextSlot = Math.min(nextSlot, index);
        return prev;
    }
    
    /**
     * Returns true if the ticket can be used to locate a resource.
     * 
     * @param ticket
     * @return 
     * @deprecated Use {@link ResourceTicket#validate(com.jme3.renderer.framegraph.ResourceTicket)} instead.
     */
    @Deprecated
    public boolean validate(ResourceTicket ticket) {
        return ResourceTicket.validate(ticket);
    }
    
    /**
     * Declares a new resource view.
     * <p>
     * The resulting {@link ResourceView} is essentially a promise of an actual concrete
     * object on demand. ResourceViews are used by resource management to schedule where
     * objects should used, and how objects can be reallocated. ResourceViews cannot be 
     * accessed directly, but can be referenced by {@link ResourceTicket}s.
     * <p>
     * If the resource is only intended to be used internally by the ResourceUser,
     * and not shared with other users, it is critical that
     * {@link #declareTemporary(codex.renthyl.resources.ResourceUser, codex.renthyl.definitions.ResourceDef, codex.renthyl.resources.ResourceTicket) declareTemporary}
     * be used instead.
     * <p>
     * If the resource does not require intensive allocation management, consider using
     * {@link #declarePrimitive(codex.renthyl.resources.ResourceUser, codex.renthyl.resources.ResourceTicket) declarePrimitive}
     * instead.
     * <p>
     * <em>Note: passing {@code null} as the resource definition marks the resource
     * as primitive. This technique was deprecated because it was too vague. Older versions
     * may still pass {@code null} to declare primitive resources, but
     * {@link #declarePrimitive(codex.renthyl.resources.ResourceUser, codex.renthyl.resources.ResourceTicket) declarePrimitive}
     * should be preferred.</em>
     * 
     * @param <T>
     * @param producer author of the resource responsible for the initial acquirement
     * @param def defines resource behavior and reallocation policies
     * @param store stores indexing information, and can be used later to retrieve the created resource
     * @return given resource ticket
     */
    public <T> ResourceTicket<T> declare(ResourceUser producer, ResourceDef<T> def, ResourceTicket<T> store) {
        ResourceView<T> resource = create(producer, def, store);
        if (cap != null) cap.declareResource(resource.getIndex(), store.getName());
        return resource.getTicket().copyIndexTo(store);
    }
    
    /**
     * Declares a new primitive resource view.
     * <p>
     * Primitive resource views directly contain the resource without involving
     * the {@link RenderObjectMap} for allocation management. This is a much more
     * niave method of handling resources, but it can often be faster for
     * low-impact resources that don't have much to gain much from reallocation.
     * 
     * @param <T>
     * @param producer
     * @param store
     * @return 
     */
    public <T> ResourceTicket<T> declarePrimitive(ResourceUser producer, ResourceTicket<T> store) {
        return declare(producer, null, store);
    }
    
    /**
     * Declares a new temporary resource.
     * <p>
     * Temporary resources can only be used by the declaring pass, and do not
     * participate in or affect culling. This is suitable for resources that
     * require allocation management but are not shared with other passes.
     * <p>
     * Attempting to reference a temporary resource will result in an exception.
     * 
     * @param <T>
     * @param producer
     * @param def
     * @param store
     * @return 
     */
    public <T> ResourceTicket<T> declareTemporary(ResourceUser producer, ResourceDef<T> def, ResourceTicket<T> store) {
        store = declare(producer, def, store);
        locate(store).setTemporary(true);
        endangeredTickets.add(store);
        return store;
    }
    
    /**
     * Reserves the object at the ticket's {@link ResourceTicket#getObjectId() object ID},
     * if valid.
     * <p>
     * Reserving an object guarantees that the object cannot be reallocated by
     * something else at the time this process would need it (defined by the pass
     * index). Any allocation request that overlaps the specified index is
     * denied.
     * <p>
     * Tickets save the ID of the last object they were involved in acquiring.
     * 
     * @param passIndex
     * @param ticket 
     */
    public void reserve(ModuleIndex passIndex, ResourceTicket ticket) {
        if (ticket.getObjectId() >= 0) {
            map.reserve(ticket.getObjectId(), passIndex);
            ticket.copyObjectTo(locate(ticket).getTicket());
        }
    }
    
    /**
     * Makes reservations at the index for each {@link RenderObject} referenced by the tickets.
     * 
     * @param passIndex
     * @param tickets 
     * @see #reserve(codex.renthyl.modules.ModuleIndex, codex.renthyl.resources.ResourceTicket)
     */
    public void reserve(ModuleIndex passIndex, ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            reserve(passIndex, t);
        }
    }
    
    private void reference(ModuleIndex index, String user, ResourceTicket ticket, boolean optional) {
        boolean sync = !frameGraph.isAsync();
        if (optional && sync && !ResourceTicket.validate(ticket)) {
            return;
        }
        ResourceView resource = locate(ticket, sync);
        if (resource != null) {
            resource.reference(index, ticket);
            if (cap != null) cap.referenceResource(resource.getIndex(), ticket.getName());
        } else {
            // save for later, since the resource hasn't been declared yet
            futureRefs.add(new FutureReference(index, ticket, optional, user));
        }
    }
    
    /**
     * References the existing resource view associated with the ticket.
     * <p>
     * Referencing indicates that the resource view is being used by an entity
     * other than its creator. References are used to determine the lifetime of
     * a resource view. Management destroys views as soon as their lifetime expires
     * (or fully {@link #release(codex.renthyl.resources.ResourceTicket) released})
     * to free up objects for reallocation.
     * <p>
     * It is critical that any entity wishing to use a resource view first reference
     * it during the appropriate stages. Additionally, all references should eventually
     * be followed by a corresponding {@link #release(codex.renthyl.resources.ResourceTicket) release}
     * call.
     * <p>
     * For asynchronous graphs, an entity may attempt to reference a resource view
     * that does not exist, but will exist later. In which case, the actual referencing
     * operation is deferred to a later time when all resource views are guaranteed
     * to have been declared. If a reference is indeed made to a non-existant resource
     * view, or the ticket is {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid},
     * an exception will be thrown at that time.
     * 
     * @param passIndex render pass index
     * @param user name of the referencing user (for debugging, may be null)
     * @param ticket ticket pointing to the resource view to reference
     */
    public void reference(ModuleIndex passIndex, String user, ResourceTicket ticket) {
        reference(passIndex, user, ticket, false);
    }
    
    /**
     * References the resource associated with the ticket if the ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) valid}.
     * 
     * @param passIndex render pass index
     * @param user
     * @param ticket
     * @see #reference(codex.renthyl.modules.ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket) 
     */
    public void referenceOptional(ModuleIndex passIndex, String user, ResourceTicket ticket) {
        reference(passIndex, user, ticket, true);
    }
    
    /**
     * References resources associated with the tickets.
     * 
     * @param passIndex render pass index
     * @param user
     * @param tickets 
     * @see #reference(codex.renthyl.modules.ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket)
     */
    public void reference(ModuleIndex passIndex, String user, ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            reference(passIndex, user, t, false);
        }
    }
    
    /**
     * References resources associated with the tickets.
     * <p>
     * Tickets that are {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid}
     * will be skipped.
     * 
     * @param passIndex render pass index
     * @param user
     * @param tickets 
     */
    public void referenceOptional(ModuleIndex passIndex, String user, ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            reference(passIndex, user, t, true);
        }
    }
    
    /**
     * Gets the definition of the resource view associated with the ticket.
     * <p>
     * An exception will be thrown if the ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid}.
     * 
     * @param <T>
     * @param <R>
     * @param type
     * @param ticket
     * @return definition of the resource view, or null if the resource view is primitive
     */
    public <T, R extends ResourceDef<T>> R getDefinition(Class<R> type, ResourceTicket<T> ticket) {
        ResourceDef<T> def = locate(ticket).getDefinition();
        if (type.isAssignableFrom(def.getClass())) {
            return (R)def;
        }
        return null;
    }
    
    /**
     * Marks the resource associated with the ticket as undefined.
     * <p>
     * Undefined resources cannot hold objects. If an undefined resource is acquired (unless with
     * {@link #acquireOrElse(com.jme3.renderer.framegraph.ResourceTicket, java.lang.Object) acquireOrElse}),
     * an exception will occur at that time.
     * <p>
     * If the given ticket is {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid},
     * an exception will be thrown.
     * 
     * @param ticket 
     */
    public void setUndefined(ResourceTicket ticket) {
        ResourceView resource = locate(ticket);
        resource.setUndefined();
        if (cap != null) cap.setResourceUndefined(resource.getIndex(), ticket.getName());
    }
    
    /**
     * Marks each resource view associated with the tickets as undefined.
     * 
     * @param tickets 
     * @see #setUndefined(codex.renthyl.resources.ResourceTicket) 
     */
    public void setUndefined(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            setUndefined(t);
        }
    }
    
    /**
     * Marks the existing, concrete object held be the resource view associated
     * with the ticket as constant.
     * <p>
     * Constant objects cannot be reallocated until the end of the frame, when they
     * are reset to not being constant. If the resource view is not associated with
     * an object, this method does nothing.
     * <p>
     * Throws an exception if the given ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid}.
     * 
     * @param ticket 
     */
    public void setConstant(ResourceTicket ticket) {
        RenderObject obj = locate(ticket).getObject();
        if (obj != null) {
            obj.setConstant(true);
            if (cap != null) cap.setObjectConstant(obj.getId());
        }
    }
    
    /**
     * Marks the resource associated with the ticket only if the given ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) valid}.
     * <p>
     * Exceptions will not be thrown for invalid tickets.
     * 
     * @param ticket 
     * @see #setConstant(codex.renthyl.resources.ResourceTicket) 
     */
    public void setConstantOptional(ResourceTicket ticket) {
        if (validate(ticket)) {
            setConstant(ticket);
        }
    }
    
    /**
     * Returns true if the resource associated with the ticket is virtual.
     * <p>
     * A resource is virtual if it does not contain a concrete object and is
     * not marked as undefined. Resources can only be virtual if they have never
     * been acquired, assigned primitively, or set to undefined.
     * 
     * @param ticket
     * @param optional
     * @return 
     */
    public boolean isVirtual(ResourceTicket ticket, boolean optional) {
        if (!optional || validate(ticket)) {
            return locate(ticket).isVirtual();
        }
        return true;
    }
    
    /**
     * Forces the current thread to wait until the resource view at the ticket is
     * available for reading, or until a timeout occurs.
     * <p>
     * A resource becomes available for reading after being released by the declaring pass.
     * Then all waiting passes may access it for reading only.
     * <p>
     * The operation is skipped without an exception being thrown if the ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid}.
     * 
     * @param ticket ticket to locate resource with
     * @param thread current thread
     * @param timeoutMillis milliseconds to wait before an exception is thrown
     */
    public void waitForReadPermission(ResourceTicket ticket, int thread, long timeoutMillis) {
        if (ResourceTicket.validate(ticket)) {
            // wait for resource to become available to this context
            long start = System.currentTimeMillis();
            ResourceView res;
            // TODO: determine why not locating the resource on each try results in timeouts
            while (!(res = fastLocate(ticket)).isReadAvailable()) {
                if (System.currentTimeMillis()-start >= timeoutMillis) {
                    throw new IllegalStateException("Thread "+thread+": Resource at "+ticket+" was assumed"
                            + " unreachable after "+timeoutMillis+" milliseconds.");
                }
            }
            // Claim read permissions. For resources that are read concurrent, this won't matter.
            if (!res.claimReadPermissions()) {
                waitForReadPermission(ticket, thread);
            }
        }
    }
    
    /**
     * Forces the current thread to wait until the resource view at the ticket
     * is available for reading, or until a timeout occurs.
     * <p>
     * A timeout occurs after {@link #WAIT_TIMEOUT} milliseconds.
     * 
     * @param ticket
     * @param thread 
     * @see #waitForReadPermission(codex.renthyl.resources.ResourceTicket, int, long) 
     */
    public void waitForReadPermission(ResourceTicket ticket, int thread) {
        waitForReadPermission(ticket, thread, WAIT_TIMEOUT);
    }
    
    /**
     * Returns true if the resource at the ticket is asynchronous.
     * <p>
     * A resource is considered asynchronous if it is referenced from multiple
     * threads. In which case the resource cannot be reallocated until all such
     * references have been released.
     * 
     * @param ticket
     * @return 
     */
    public boolean isAsync(ResourceTicket ticket) {
        if (ResourceTicket.validate(ticket)) {
            return locate(ticket).getLifeTime().isAsync();
        }
        return false;
    }
    
    /**
     * Acquires the object held by the given resource.
     * <p>
     * If the resource view is {@link ResourceView#isVirtual() virtual},
     * one will either be created or reallocated by the {@link RenderObjectMap}.
     * Otherwise, the object currently associated with the resource view is returned.
     * <p>
     * The object's ID is written to the ticket, allowing the ticket to directly reference
     * the object, which is important for reserving.
     * 
     * @param <T>
     * @param resource
     * @param ticket ticket referencing the resource view
     * @return object associated with the resource view
     */
    protected <T> T acquire(ResourceView<T> resource, ResourceTicket<T> ticket) {
        if (!resource.isUsed()) {
            throw new IllegalStateException(resource + " was unexpectedly acquired.");
        }
        if (resource.isVirtual()) {
            map.allocate(resource, frameGraph.isAsync());
        }
        if (cap != null) cap.acquireResource(resource.getIndex(), ticket.getName());
        resource.getTicket().copyObjectTo(ticket);
        return resource.getResource();
    }
    
    /**
     * Acquires the object held by the given resource view.
     * <p>
     * If the resource view is {@link ResourceView#isVirtual() virtual},
     * one will either be created or reallocated by the {@link RenderObjectMap}.
     * Otherwise, the object currently associated with the resource view is returned.
     * <p>
     * The object's ID is written to the ticket, allowing the ticket to directly reference
     * the object, which is important for reserving.
     * <p>
     * An exception is thrown if the given ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid} or
     * the resource view is {@link ResourceView#isPrimitive() primitive}.
     * 
     * @param <T>
     * @param ticket ticket referencing the resource view
     * @return object associated with the resource view
     */
    public <T> T acquire(ResourceTicket<T> ticket) {
        ResourceView<T> resource = locate(ticket);
        if (resource.isUndefined()) {
            throw new NullPointerException("Cannot acquire undefined resource.");
        }
        return acquire(resource, ticket);
    }
    
    /**
     * Acquires the object held by the resource view, or returns the given
     * default value if the ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid}.
     * 
     * @param <T>
     * @param ticket
     * @param value default value (may be null)
     * @return acquired object or the default value
     * @see #acquire(codex.renthyl.resources.ResourceTicket) 
     */
    public <T> T acquireOrElse(ResourceTicket<T> ticket, T value) {
        if (ResourceTicket.validate(ticket)) {
            ResourceView<T> resource = locate(ticket);
            if (!resource.isUndefined()) {
                return acquire(resource, ticket);
            }
        }
        return value;
    }
    
    /**
     * Acquires textures and assigns them as color targets to the framebuffer.
     * <p>
     * If a texture is already assigned to the framebuffer at the same color target index,
     * then nothing will be changed at that index.
     * <p>
     * Existing texture targets beyond the number of tickets passed will be removed.
     * 
     * @param fbo framebuffer to assign color targets to
     * @param tickets tickets referencing resource views
     * @see #acquire(codex.renthyl.resources.ResourceTicket)
     */
    public void acquireColorTargets(FrameBuffer fbo, ResourceTicket<? extends Texture>... tickets) {
        acquireColorTargets(fbo, null, tickets);
    }
    
    /**
     * Acquires textures and assigns them as color targets to the framebuffer.
     * <p>
     * Acquired textures are stored in the given texture array.
     * 
     * @param fbo framebuffer to assign color targets to
     * @param texArray array to populate with acquired textures, or null
     * @param tickets array of tickets referencing resource views (must be same length as {@code texArray})
     * @return populated texture array
     * @see #acquire(codex.renthyl.resources.ResourceTicket) 
     */
    public Texture[] acquireColorTargets(FrameBuffer fbo, Texture[] texArray, ResourceTicket<? extends Texture>[] tickets) {
        if (tickets.length == 0) {
            fbo.clearColorTargets();
            fbo.setUpdateNeeded();
            return texArray;
        }
        while (tickets.length < fbo.getNumColorTargets()) {
            fbo.removeColorTarget(fbo.getNumColorTargets()-1);
            fbo.setUpdateNeeded();
        }
        int i = 0;
        for (int n = Math.min(fbo.getNumColorTargets(), tickets.length); i < n; i++) {
            Texture t = replaceColorTarget(fbo, tickets[i], i);
            if (texArray != null) {
                texArray[i] = t;
            }
        }
        for (; i < tickets.length; i++) {
            Texture t = acquire(tickets[i]);
            if (texArray != null) {
                texArray[i] = t;
            }
            fbo.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(t));
            fbo.setUpdateNeeded();
            if (cap != null) cap.bindTexture(tickets[i].getWorldIndex(), tickets[i].getName());
            textureBinds++;
        }
        return texArray;
    }
    
    /**
     * Acquires the texture associated with the ticket and assigns it to the framebuffer.
     * 
     * @param <T>
     * @param fbo framebuffer to assign the color target to
     * @param ticket ticket referencing the desired resource view
     * @return acquired texture
     * @see #acquire(codex.renthyl.resources.ResourceTicket) 
     */
    public <T extends Texture> T acquireColorTarget(FrameBuffer fbo, ResourceTicket<T> ticket) {
        if (ticket == null) {
            if (fbo.getNumColorTargets() > 0) {
                fbo.clearColorTargets();
                fbo.setUpdateNeeded();
            }
            return null;
        }
        while (fbo.getNumColorTargets() > 1) {
            fbo.removeColorTarget(fbo.getNumColorTargets()-1);
            fbo.setUpdateNeeded();
        }
        return replaceColorTarget(fbo, ticket, 0);
    }
    
    private <T extends Texture> T replaceColorTarget(FrameBuffer fbo, ResourceTicket<T> ticket, int i) {
        if (i < fbo.getNumColorTargets()) {
            Texture existing = fbo.getColorTarget(i).getTexture();
            T acquired = acquire(ticket);
            if (acquired != existing) {
                fbo.replaceColorTarget(i, FrameBuffer.FrameBufferTarget.newTarget(acquired));
                fbo.setUpdateNeeded();
                if (cap != null) cap.bindTexture(ticket.getWorldIndex(), ticket.getName());
                textureBinds++;
            }
            return acquired;
        } else {
            T acquired = acquire(ticket);
            fbo.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(acquired));
            fbo.setUpdateNeeded();
            return acquired;
        }
    }
    
    /**
     * Acquires the texture associated with the ticket and assigns it as the depth
     * target to the framebuffer.
     * <p>
     * If the texture is already assigned to the framebuffer as the depth target,
     * the nothing changes.
     * 
     * @param <T>
     * @param fbo framebuffer to assign the depth target to
     * @param ticket ticket referencing the desired resource view
     * @return acquired texture
     * @see #acquire(codex.renthyl.resources.ResourceTicket) 
     */
    public <T extends Texture> T acquireDepthTarget(FrameBuffer fbo, ResourceTicket<T> ticket) {
        T acquired = acquire(ticket);
        FrameBuffer.RenderBuffer target = fbo.getDepthTarget();
        if (target == null || acquired != target.getTexture()) {
            fbo.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(acquired));
            fbo.setUpdateNeeded();
            if (cap != null) cap.bindTexture(ticket.getWorldIndex(), ticket.getName());
            textureBinds++;
        }
        return acquired;
    }
    
    /**
     * Acquires a cached object at the key.
     * <p>
     * The acquired object is removed from the cache and put back into regular
     * management by {@link RenderObjectMap}.
     * <p>
     * If no cached object exists at the key, the resource at the ticket will be
     * set to undefined. If the resource is not virtual, an exception will be thrown.
     * <p>
     * This operation is not threadsafe if two threads request the same resource at once.
     * 
     * @param <T>
     * @param ticket resource view to associate the cached object with
     * @param key
     * @return cached object, or null if none exists at the key
     * @see #cache(codex.renthyl.resources.ResourceTicket, java.lang.String)
     */
    public <T> T acquireCached(ResourceTicket ticket, String key) {
        ResourceView<T> res = locate(ticket);
        if (!res.isVirtual()) {
            throw new IllegalStateException(res+" must be virtual to acquire cached resource.");
        }
        if (map.allocateFromCache(cache, res, key)) {
            return res.getResource();
        }
        res.setUndefined();
        return null;
    }
    
    /**
     * Transfers the resource associated with {@code from} to the ResourceView
     * associated with {@code to}.This operation is intended to replace the initial
    {@link #acquire(codex.renthyl.resources.ResourceTicket) acquire} call for
 the target ResourceView.<p>
     * In order for a transfer to be successful:
     * <ul>
     *   <li>Both tickets must be valid and point to existing ResourceViews.</li>
     *   <li>The source ResourceView must not be {@link #isVirtual(codex.renthyl.resources.ResourceTicket, boolean) virtual}.</li>
     *   <li>The target ResourceView must be virtual.</li>
     *   <li>The source ResourceView must be {@link ResourceView#isPartiallyReleased() partially released}.</li>
     *   <li>The transfering object must not be destroyed by the source ResourceView's definition.</li>
     *   <li>The transfering object must not be {@link #setConstant(codex.renthyl.resources.ResourceTicket) constant}.</li>
     *   <li>The target ResourceView's definition must not reject the object.</li>
     * </ul>
     * Otherwise an exception will be thrown. This method automatically releases
     * the {@code from} ticket from the source ResourceView, after which the resource
     * is expected to be {@link ResourceView#isFullyReleased() fully released}.
     * <p>
     * Because this method fails when an adverse situation arises rather than deferring
     * as {@link #acquire(codex.renthyl.resources.ResourceTicket) acquiring} would do,
     * this method ignores reservations held to the transfering object.
     * 
     * @param <T>
     * @param sourceTicket represents the ResourceView to transfer from
     * @param targetTicket represents the ResourceView to transfer to
     * @return the transfered resource
     */
    public <T> T modify(ResourceTicket sourceTicket, ResourceTicket<T> targetTicket) {
        ResourceView source = locate(sourceTicket);
        ResourceView<T> target = locate(targetTicket);
        if (source.isVirtual()) {
            throw new IllegalStateException("Source resource must not be virtual to accept modification.");
        }
        if (!target.isVirtual()) {
            throw new IllegalStateException("Target resource must be virtual to accept modification.");
        }
        // copy over object id so that specific allocation will target the correct resource
        source.getTicket().copyObjectTo(target.getTicket());
        // release the resource
        if (!release(sourceTicket)) {
            throw new IllegalStateException(source + " must be fully released to modify.");
        }
        // immediately acquire the resource again
        if (!map.allocateSpecific(target, frameGraph.isAsync(), true)) {
            throw new NullPointerException("Unable to complete modification of " + source + " to " + target);
        }
        // copy the object id to the target ticket provided by the user
        target.getTicket().copyObjectTo(targetTicket);
        return target.getResource();
    }
    
    /**
     * Returns true if the resource view associated with the ticket
     * is available for {@link #acquire(codex.renthyl.resources.ResourceTicket) acquiring}.
     * 
     * @param ticket
     * @return 
     */
    public boolean available(ResourceTicket ticket) {
        return acquireOrElse(ticket, null) != null;
    }
    
    /**
     * Directly assigns the resource view associated with the ticket to the value.
     * <p>
     * A render object is not created to store the value. Instead, the render resource
     * will directly store the value. Note that the value will be lost when the
     * render resource is destroyed at the end of the rendering process.
     * <p>
     * A resource should only be set primitively if it was
     * {@link #declarePrimitive(codex.renthyl.resources.ResourceUser, codex.renthyl.resources.ResourceTicket) declared primitively},
     * otherwise an exception will be thrown.
     * <p>
     * This is intended to called in place of {@link #acquire(com.jme3.renderer.framegraph.ResourceTicket)}.
     * 
     * @param <T>
     * @param ticket 
     * @param value 
     */
    public <T> void setPrimitive(ResourceTicket<T> ticket, T value) {
        locate(ticket).setPrimitive(value);
    }
    
    /**
     * 
     * @param resource
     * @param ticket
     * @return 
     * @see #release(codex.renthyl.resources.ResourceTicket) 
     */
    protected boolean release(ResourceView resource, ResourceTicket ticket) {
        if (cap != null) cap.releaseResource(resource.getIndex(), resource.getTicket().getName());
        if (!resource.release(this, ticket)) {
            if (cap != null && resource.getObject() != null) {
                cap.releaseObject(resource.getObject().getId());
            }
            remove(resource.getIndex());
            if (!resource.isVirtual() && !resource.isUndefined()) {
                ResourceDef def = resource.getDefinition();
                if (def != null && def.isDisposeOnRelease()) {
                    map.dispose(resource);
                }
            }
        }
        return resource.isFullyReleased();
    }
    
    /**
     * Releases the resource view from use.
     * <p>
     * It is critical that entities that {@link #declare(codex.renthyl.resources.ResourceUser, codex.renthyl.definitions.ResourceDef, codex.renthyl.resources.ResourceTicket) declare}
     * or {@link #reference(codex.renthyl.modules.ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket) reference}
     * a resource also release it once finished with it. Entities that declare a resource
     * should not release a resource without first {@link #acquire(codex.renthyl.resources.ResourceTicket) acquiring} it.
     * <p>
     * Once {@link ResourceView#isFullyReleased() fully released} from use by all declaring and referencing entities, the
     * resource view is destroyed and, if the resource view was not
     * {@link ResourceView#isPrimitive() primitive}, the associated object becomes
     * available for reallocation.
     * <p>
     * An exception is thrown if the given ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid}.
     * <p>
     * <em>Note: {@link codex.renthyl.modules.RenderPass RenderPass} automatically releases
     * resources associated with registered input and output tickets, however, multiple
     * release calls with the same ticket are handled gracefully.</em>
     * 
     * @param ticket 
     * @return true if the resource view has been fully released
     */
    public boolean release(ResourceTicket ticket) {
        ResourceView resource = locate(ticket);
        if (ticket.isBindFlagSet()) {
            release(resource, ticket);
        }
        ticket.clearBindFlag();
        return resource.isFullyReleased();
    }
    
    /**
     * Releases the resource view from use only if the ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) valid}.
     * <p>
     * An exception is not thrown if the ticket is invalid.
     * 
     * @param ticket
     * @return true if the resource view was fully released and the ticket is valid
     * @see #release(codex.renthyl.resources.ResourceTicket) 
     */
    public boolean releaseOptional(ResourceTicket ticket) {
        if (ResourceTicket.validate(ticket)) {
            return release(ticket);
        }
        return false;
    }
    
    /**
     * Releases the resource views use.
     * 
     * @param tickets 
     * @see #release(codex.renthyl.resources.ResourceTicket) 
     */
    public void release(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            release(t);
        }
    }
    
    /**
     * Releases the resources obtained by the tickets.
     * <p>
     * Tickets that are {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid}
     * are skipped without throwing an exception.
     * 
     * @param tickets 
     * @see #release(codex.renthyl.resources.ResourceTicket) 
     */
    public void releaseOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            releaseOptional(t);
        }
    }
    
    /**
     * Caches the object currently associated with the ticket's resource view.
     * <p>
     * The object associated with the resource view is saved to this resource
     * list's local cache, and removed from regular management in {@link RenderObjectMap}.
     * The object cannot be reallocated or accessed except through
     * {@link #acquireCached(codex.renthyl.resources.ResourceTicket, java.lang.String) acquireCached}.
     * <p>
     * Cached objects can still be destroyed after several frames of not being used.
     * <p>
     * An exception is thrown if the given ticket is
     * {@link ResourceTicket#validate(codex.renthyl.resources.ResourceTicket) invalid},
     * or the resource is virtual or primitive.
     * 
     * @param ticket 
     * @param key 
     */
    public void cache(ResourceTicket ticket, String key) {
        ResourceView res = locate(ticket);
        if (res.isVirtual()) {
            throw new IllegalStateException("Cannot cache because resource is virtual.");
        }
        if (res.isPrimitive()) {
            throw new IllegalStateException("Cannot cache primitive resource.");
        }
        map.cache(cache, res.getObject().getId(), key);
    }
    
    /**
     * Prepares this resource list for rendering.
     * <p>
     * This should only be called once per frame.
     * 
     * @param map
     * @param cap
     */
    public void beginRenderFrame(RenderObjectMap map, GraphEventCapture cap) {
        this.map = map;
        this.cap = cap;
        textureBinds = 0;
    }
    
    /**
     * Cleans up after rendering.
     */
    public void endRenderFrame() {
        map.flushCache(cache);
    }
    
    /**
     * Applies all missed references.
     */
    public void applyFutureReferences() {
        // apply registered future references
        for (FutureReference ref : futureRefs) {
            if (!ref.optional && !ResourceTicket.validate(ref.ticket)) {
                throw new NullPointerException(ref.ticket + " from " + ref.user + " is invalid.");
            }
            locate(ref.ticket).reference(ref.index, ref.ticket);
        }
        futureRefs.clear();
    }
    
    /**
     * Culls all resources and resource producers found to be unused.
     * <p>
     * This should only be called after resource users have fully counted their
     * references, and prior to execution.
     */
    public void cullUnreferenced() {
        LinkedList<ResourceView> cull = new LinkedList<>();
        // queue all resources that are not referenced
        for (ResourceView r : resources) {
            if (r != null && !r.isReferenced() && !r.isTemporary()) {
                cull.add(r);
            }
        }
        // recursively "dereference" users of unused resources
        ResourceView resource;
        while ((resource = cull.pollFirst()) != null) {
            // dereference producer of resource
            ResourceUser user = resource.getProducer();
            if (user == null) {
                remove(resource.getIndex());
                continue;
            }
            // make sure this hasn't been culled already
            if (!user.isUsed()) {
                continue;
            }
            user.dereference();
            // If the user is found to not produce used resources, dereference
            // all incoming resources and remove all outgoing resources.
            if (!user.isUsed()) {
                for (ResourceTicket t : user.getInputTickets()) {
                    t.clearBindFlag();
                    // make sure the resource still exists
                    if (!ResourceTicket.validate(t)) {
                        continue;
                    }
                    ResourceView r = locate(t);
                    // If the resource is found to no longer be referenced by
                    // anything, queue the resource.
                    if (!r.cull()) {
                        cull.addLast(r);
                    }
                }
                // remove output resource views
                for (ResourceTicket t : user.getOutputTickets()) {
                    t.clearBindFlag();
                    if (!t.hasSource()) {
                        remove(t.getLocalIndex());
                    }
                }
            }
        }
    }
    
    /**
     * Resets the resource list.
     */
    public void reset() {
        for (ResourceTicket t : endangeredTickets) {
            t.clearBindFlag();
        }
        int size = resources.size();
        resources.clear();
        nextSlot = 0;
        if (cap != null) {
            cap.clearResources(size);
            cap.value("framebufferTextureBinds", textureBinds);
        }
    }
    
    /**
     * Gets the number of known texture binds that occured during
     * the last render frame.
     * 
     * @return 
     */
    public int getNumTextureBinds() {
        return textureBinds;
    }
    
    /**
     * Returns the size of the object cache.
     * 
     * @return 
     */
    public int getObjectCacheSize() {
        return cache.size();
    }
    
    /**
     * Represents a reference to a resource that will exist in the future.
     */
    private static class FutureReference {
        
        public final ModuleIndex index;
        public final ResourceTicket ticket;
        public final boolean optional;
        public final String user;

        public FutureReference(ModuleIndex index, ResourceTicket ticket, boolean optional, String user) {
            this.index = index;
            this.ticket = ticket;
            this.optional = optional;
            this.user = user;
        }
        
    }
    
}
