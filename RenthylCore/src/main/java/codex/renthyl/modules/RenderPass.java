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
package codex.renthyl.modules;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.resources.ResourceList;
import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.util.MappedCache;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.texture.FrameBuffer;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.IntFunction;
import codex.renthyl.resources.tickets.TicketGroup;

/**
 * Modular rendering process for a {@link FrameGraph}.
 * 
 * @author codex
 */
public abstract class RenderPass extends AbstractRenderModule implements Savable {
    
    private final FrameBufferCache frameBuffers = new FrameBufferCache(
            new FrameBufferParameters(null, 1024, 1024, 1));
    protected ResourceList resources;
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        write(ex.getCapsule(this));
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        read(im.getCapsule(this));
    }
    
    @Override
    public void initializeModule(FrameGraph frameGraph) {
        super.initializeModule(frameGraph);
        initialize(frameGraph);
    }
    @Override
    public void prepareRender(FGRenderContext context) {
        super.prepareRender(context);
        resources = context.getResources();
        prepare(context);
    }
    @Override
    public void executeRender(FGRenderContext context) {
        super.executeRender(context);
        // permissions are always required
        //if (context.isAsync()) {
            claimResourcePermissions();
        //}
        execute(context);
        releaseAll();
        if (index.isMainThread()) {
            context.popActiveModes();
        }
    }
    @Override
    public void resetRender(FGRenderContext context) {
        reset(context);
        frameBuffers.flush();
    }
    @Override
    public void cleanupModule() {
        cleanup(frameGraph);
        frameBuffers.clear();
        this.frameGraph = null;
    }
    @Override
    public void renderingComplete() {}
    
    /**
     * Initializes the pass.
     * <p>
     * Tickets should be created add registered here.
     * 
     * @param frameGraph 
     */
    protected abstract void initialize(FrameGraph frameGraph);
    /**
     * Prepares the pass.
     * <p>
     * Resource should be declared, referenced, and reserved here.
     * 
     * @param context 
     */
    protected abstract void prepare(FGRenderContext context);
    /**
     * Executes the pass.
     * <p>
     * All declared and referenced resources should be acquired here. Resources
     * must also be released, but that occurs automatically.
     * 
     * @param context 
     */
    protected abstract void execute(FGRenderContext context);
    /**
     * Resets the pass.
     * 
     * @param context 
     */
    protected abstract void reset(FGRenderContext context);
    /**
     * Cleans up the pass.
     * 
     * @param frameGraph 
     */
    protected abstract void cleanup(FrameGraph frameGraph);
    
    /**
     * Declares a new resource using a registered ticket.
     * 
     * @param <T>
     * @param def definition for new resource
     * @param ticket ticket to store resulting index
     * @return given ticket
     * @see ResourceList#declare(codex.renthyl.resources.ResourceUser, codex.renthyl.definitions.ResourceDef, codex.renthyl.resources.ResourceTicket)
     */
    protected <T> ResourceTicket<T> declare(ResourceDef<T> def, ResourceTicket<T> ticket) {
        return resources.declare(this, def, ticket);
    }
    /**
     * Declares a new primitive resource.
     * 
     * @param ticket
     * @return 
     * @see ResourceList#declarePrimitive(codex.renthyl.resources.ResourceUser, codex.renthyl.resources.ResourceTicket)
     */
    protected ResourceTicket declarePrimitive(ResourceTicket ticket) {
        return resources.declarePrimitive(this, ticket);
    }
    /**
     * 
     * @param tickets
     * @return 
     * @see ResourceList#declarePrimitive(codex.renthyl.resources.ResourceUser, codex.renthyl.resources.ResourceTicket)
     */
    protected ResourceTicket[] declarePrimitive(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            resources.declarePrimitive(this, t);
        }
        return tickets;
    }
    /**
     * 
     * @param tickets
     * @see ResourceList#declarePrimitive(codex.renthyl.resources.ResourceUser, codex.renthyl.resources.ResourceTicket)
     */
    protected void declarePrimitive(Iterable<? extends ResourceTicket> tickets) {
        for (ResourceTicket t : tickets) {
            resources.declarePrimitive(this, t);
        }
    }
    /**
     * Declares a new temporary resource using an unregistered ticket.
     * 
     * @param <T>
     * @param def
     * @param ticket
     * @return 
     * @see ResourceList#declareTemporary(codex.renthyl.resources.ResourceUser, codex.renthyl.definitions.ResourceDef, codex.renthyl.resources.ResourceTicket)
     */
    protected <T> ResourceTicket<T> declareTemporary(ResourceDef<T> def, ResourceTicket<T> ticket) {
        return resources.declareTemporary(this, def, ticket);
    }
    /**
     * Reserves the {@link com.jme3.renderer.framegraph.RenderObject RenderObject} associated with the ticket.
     * 
     * @param ticket 
     * @see ResourceList#reserve(ModuleIndex, codex.renthyl.resources.ResourceTicket)
     */
    protected void reserve(ResourceTicket ticket) {
        resources.reserve(index, ticket);
    }
    /**
     * Reserves each RenderObject associated with the tickets.
     * 
     * @param tickets
     * @see ResourceList#reserve(ModuleIndex, codex.renthyl.resources.ResourceTicket)
     */
    protected void reserve(ResourceTicket... tickets) {
        resources.reserve(index, tickets);
    }
    /**
     * Reserves each RenderObject associated with the tickets.
     * 
     * @param tickets
     * @see ResourceList#reserve(ModuleIndex, codex.renthyl.resources.ResourceTicket)
     */
    protected void reserve(Iterable<? extends ResourceTicket> tickets) {
        resources.reserve(index, tickets);
    }
    /**
     * References the resource associated with the ticket.
     * 
     * @param ticket 
     * @see ResourceList#reference(ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket)
     */
    protected void reference(ResourceTicket ticket) {
        resources.reference(index, name, ticket);
    }
    /**
     * References each resource associated with the tickets.
     * 
     * @param tickets 
     * @see ResourceList#reference(ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket)
     */
    protected void reference(ResourceTicket... tickets) {
        resources.reference(index, name, tickets);
    }
    /**
     * References each resource associated with the tickets.
     * 
     * @param tickets 
     * @see ResourceList#reference(ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket)
     */
    protected void reference(Iterable<? extends ResourceTicket> tickets) {
        resources.reference(index, name, tickets);
    }
    /**
     * References the resource associated with the ticket if the ticket is not
     * null and contains a non-negative world index.
     * 
     * @param ticket
     * @see ResourceList#referenceOptional(ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket)
     */
    protected void referenceOptional(ResourceTicket ticket) {
        resources.referenceOptional(index, name, ticket);
    }
    /**
     * Optionally references each resource associated with the tickets.
     * 
     * @param tickets 
     * @see ResourceList#referenceOptional(ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket)
     */
    protected void referenceOptional(ResourceTicket... tickets) {
        resources.referenceOptional(index, name, tickets);
    }
    /**
     * Optionally references each resource associated with the tickets.
     * 
     * @param tickets 
     * @see ResourceList#referenceOptional(ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket)
     */
    protected void referenceOptional(Iterable<? extends ResourceTicket> tickets) {
        resources.referenceOptional(index, name, tickets);
    }
    
    /**
     * Claims read and write permissions for input and output resources,
     * respectively, before exiting.
     * <p>
     * The current thread is blocked until the necessary permissions are acquired.
     */
    public void claimResourcePermissions() {
        for (TicketGroup<Object> g : outputGroups.values()) {
            for (ResourceTicket t : g.getTickets()) {
                resources.claimWritePermissions(t);
            }
        }
        for (TicketGroup<Object> g : inputGroups.values()) {
            for (ResourceTicket t : g.getTickets()) {
                resources.waitForReadPermission(t);
            }
        }
    }
    
    /**
     * Acquires a add of resources from a ticket group and stores them in the array.
     * 
     * @param <T>
     * @param group
     * @param array
     * @return 
     * @see ResourceList#acquire(codex.renthyl.resources.ResourceTicket) 
     */
    protected <T> T[] acquireArray(TicketGroup<T> group, T[] array) {
        int i = 0;
        for (ResourceTicket<T> t : group.getTickets()) {
            if (i >= array.length) {
                break;
            }
            array[i++] = resources.acquire(t);
        }
        return array;
    }
    /**
     * Acquires a add of resources from a ticket group and stores them in an
     * array created by the function.
     * 
     * @param <T>
     * @param group
     * @param func
     * @return 
     */
    protected <T> T[] acquireArray(TicketGroup<T> group, IntFunction<T[]> func) {
        int i = 0;
        T[] array = func.apply(group.size());
        for (ResourceTicket<T> t : group.getTickets()) {
            if (i >= array.length) {
                break;
            }
            array[i++] = resources.acquire(t);
        }
        return array;
    }
    /**
     * Acquires a add of resources from a ticket group and stores them in the array.
     * <p>
     * Tickets that are invalid will acquire {@code val} instead.
     * 
     * @param <T>
     * @param group
     * @param array
     * @param val
     * @return 
     * @see ResourceList#acquireOrElse(codex.renthyl.resources.ResourceTicket, java.lang.Object) 
     */
    protected <T> T[] acquireArrayOrElse(TicketGroup<T> group, T[] array, T val) {
        int i = 0;
        for (ResourceTicket<T> t : group.getTickets()) {
            if (i >= array.length) {
                break;
            }
            array[i++] = resources.acquireOrElse(t, val);
        }
        return array;
    }
    /**
     * Acquires a add of resources from a ticket group and stores them in an
     * array created by the function.
     * <p>
     * Tickets that are invalid will acquire {@code val}.
     * 
     * @param <T>
     * @param group
     * @param func
     * @param val
     * @return 
     * @see ResourceList#acquireOrElse(codex.renthyl.resources.ResourceTicket, java.lang.Object) 
     */
    protected <T> T[] acquireArrayOrElse(TicketGroup<T> group, IntFunction<T[]> func, T val) {
        int i = 0;
        T[] array = func.apply(group.size());
        for (ResourceTicket<T> t : group.getTickets()) {
            if (i >= array.length) {
                break;
            }
            array[i++] = resources.acquireOrElse(t, val);
        }
        return array;
    }
    /**
     * Acquires a list of resources from a ticket group and stores them in the given list.
     * 
     * @param <T>
     * @param <R>
     * @param group
     * @param collection list to store resources in (or null to create a new {@link LinkedList}).
     * @return given list
     * @see ResourceList#acquire(codex.renthyl.resources.ResourceTicket)
     */
    protected <T, R extends Collection<T>> R acquireList(TicketGroup<T> group, R collection) {
        Objects.requireNonNull(collection, "Collection to store acquired resources cannot be null.");
        for (ResourceTicket<T> t : group.getTickets()) {
            T res = resources.acquireOrElse(t, null);
            if (res != null) collection.add(res);
        }
        return collection;
    }
    /**
     * Acquires a list of resources from a ticket group and stores them in the given list.
     * 
     * @param <T>
     * @param group
     * @return given list
     * @see ResourceList#acquire(codex.renthyl.resources.ResourceTicket)
     */
    protected <T> Collection<T> acquireList(TicketGroup<T> group) {
        return acquireList(group, new LinkedList<>());
    }
    
    /**
     * Releases all reasources associated with any registered ticket.
     * <p>
     * Called automatically after execution if {@link #autoTicketRelease} is true.
     * 
     * @see ResourceList#release(codex.renthyl.resources.ResourceTicket) 
     */
    protected void releaseAll() {
        for (TicketGroup g : inputGroups.values()) {
            g.releaseAll(resources);
        }
        for (TicketGroup g : outputGroups.values()) {
            g.releaseAll(resources);
        }
    }
    
    /**
     * Gets an existing {@link FrameBuffer} that matches the given properties.
     * <p>
     * If no existing FrameBuffer matches, a new framebuffer will be created
     * and returned. FrameBuffers that are not used during pass execution
     * are disposed.
     * <p>
     * If the event capturer is not null, an event will be logged for debugging.
     * 
     * @param tag tag (name) requirement for returned FrameBuffer (may be null)
     * @param width width requirement for returned FrameBuffer
     * @param height height requirement for returned FrameBuffer
     * @param samples samples requirement for returned FrameBuffer
     * @return FrameBuffer matching given width, height, and samples
     */
    protected FrameBuffer getFrameBuffer(Object tag, int width, int height, int samples) {
        FrameBufferParameters params = frameBuffers.getLocalKey();
        params.tag = tag;
        params.width = width;
        params.height = height;
        params.samples = samples;
        return frameBuffers.fetch();
    }
    /**
     * 
     * @param width
     * @param height
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(int width, int height, int samples) {
        return getFrameBuffer(null, width, height, samples);
    }
    /**
     * Creates a FrameBuffer matching the width and height presented by the {@link FGRenderContext}.
     * 
     * @param context
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(FGRenderContext context, int samples) {
        return getFrameBuffer(context.getWidth(), context.getHeight(), samples);
    }
    /**
     * Creates a FrameBuffer matching the width and height presented by the {@link FGRenderContext}.
     * 
     * @param context
     * @param tag
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(FGRenderContext context, Object tag, int samples) {
        return getFrameBuffer(tag, context.getWidth(), context.getHeight(), samples);
    }
    
    /**
     * Gets the name given to a profiler, which may be more compact or informative.
     * 
     * @return 
     */
    public String getProfilerName() {
        return getName();
    }
    
    /**
     * Convenience method for writing pass properties to the output capsule.
     * 
     * @param out
     * @throws IOException 
     */
    protected void write(OutputCapsule out) throws IOException {}
    /**
     * Convenience method for reading pass properties from the input capsule.
     * 
     * @param in
     * @throws IOException 
     */
    protected void read(InputCapsule in) throws IOException {}
    
    private static class FrameBufferCache extends MappedCache<FrameBufferParameters, FrameBuffer> {

        public FrameBufferCache() {}
        public FrameBufferCache(FrameBufferParameters localKey) {
            super(localKey);
        }
        
        @Override
        protected FrameBuffer createElement(FrameBufferParameters key) {
            return new FrameBuffer(key.width, key.height, key.samples);
        }
        @Override
        protected void destroyElement(FrameBuffer element) {
            element.dispose();
        }
        
    }
    private static class FrameBufferParameters {
        
        private Object tag;
        private int width, height, samples;

        public FrameBufferParameters(Object tag, int width, int height, int samples) {
            this.tag = tag;
            this.width = width;
            this.height = height;
            this.samples = samples;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.tag);
            hash = 97 * hash + this.width;
            hash = 97 * hash + this.height;
            hash = 97 * hash + this.samples;
            return hash;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FrameBufferParameters other = (FrameBufferParameters) obj;
            if (this.width != other.width) {
                return false;
            }
            if (this.height != other.height) {
                return false;
            }
            if (this.samples != other.samples) {
                return false;
            }
            return Objects.equals(this.tag, other.tag);
        }
        
    }
    
}
