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
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.resources.tickets.TicketGroup;
import codex.renthyl.definitions.ResourceDef;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Modular rendering process for a {@link FrameGraph}.
 * 
 * @author codex
 */
public abstract class RenderPass extends RenderModule implements Savable {
    
    private final FrameBufferCache frameBuffers = new FrameBufferCache(new FrameBufferParameters(null, 1024, 1024, 1));
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
    public void traverse(Consumer<RenderModule> traverser) {
        traverser.accept(this);
    }
    
    @Override
    public void initModule(FrameGraph frameGraph) {
        initialize(frameGraph);
    }
    @Override
    public void prepareModuleRender(FGRenderContext context) {
        resources = context.getResources();
        prepare(context);
    }
    @Override
    public void executeRender(FGRenderContext context) {
        if (context.isAsync()) {
            waitToExecute();
        }
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
    public void cleanupModule(FrameGraph frameGraph) {
        cleanup(frameGraph);
        inputs.clear();
        outputs.clear();
        groups.clear();
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
     * @see ResourceList#reserve(codex.renthyl.modules.ModuleIndex, codex.renthyl.resources.ResourceTicket)
     */
    protected void reserve(ResourceTicket ticket) {
        resources.reserve(index, ticket);
    }
    /**
     * Reserves each RenderObject associated with the tickets.
     * 
     * @param tickets
     * @see ResourceList#reserve(codex.renthyl.modules.ModuleIndex, codex.renthyl.resources.ResourceTicket) 
     */
    protected void reserve(ResourceTicket... tickets) {
        resources.reserve(index, tickets);
    }
    /**
     * References the resource associated with the ticket.
     * 
     * @param ticket 
     * @see ResourceList#reference(codex.renthyl.modules.ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket) 
     */
    protected void reference(ResourceTicket ticket) {
        resources.reference(index, name, ticket);
    }
    /**
     * References each resource associated with the tickets.
     * 
     * @param tickets 
     * @see ResourceList#reference(codex.renthyl.modules.ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket) 
     */
    protected void reference(ResourceTicket... tickets) {
        resources.reference(index, name, tickets);
    }
    /**
     * References the resource associated with the ticket if the ticket is not
     * null and contains a non-negative world index.
     * 
     * @param ticket
     * @see ResourceList#referenceOptional(codex.renthyl.modules.ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket) 
     */
    protected void referenceOptional(ResourceTicket ticket) {
        resources.referenceOptional(index, name, ticket);
    }
    /**
     * Optionally references each resource associated with the tickets.
     * 
     * @param tickets 
     * @see ResourceList#referenceOptional(codex.renthyl.modules.ModuleIndex, java.lang.String, codex.renthyl.resources.ResourceTicket) 
     */
    protected void referenceOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            referenceOptional(t);
        }
    }
    
    /**
     * Forces this thread to wait until all inputs are available for this pass.
     * <p>
     * An incoming resource is deemed ready when {@link com.jme3.renderer.framegraph.ResourceView#claimReadPermissions()
     * read permissions are claimed}.
     */
    public void waitToExecute() {
        for (ResourceTicket t : inputs) {
            resources.waitForReadPermission(t, index.threadIndex);
        }
    }
    
    /**
     * Acquires a set of resources from a ticket group and stores them in the array.
     * 
     * @param <T>
     * @param name
     * @param array
     * @return 
     * @see ResourceList#acquire(codex.renthyl.resources.ResourceTicket) 
     */
    protected <T> T[] acquireArray(String name, T[] array) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquire(tickets[i]);
        }
        return array;
    }
    /**
     * Acquires a set of resources from a ticket group and stores them in an
     * array created by the function.
     * <p>
     * The function is expected to create an array of the given length.
     * 
     * @param <T>
     * @param name
     * @param func
     * @return created array
     * @see ResourceList#acquire(codex.renthyl.resources.ResourceTicket) 
     */
    protected <T> T[] acquireArray(String name, Function<Integer, T[]> func) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        T[] array = func.apply(tickets.length);
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquire(tickets[i]);
        }
        return array;
    }
    /**
     * Acquires a set of resources from a ticket group and stores them in
     * the array.
     * <p>
     * Tickets that are invalid will acquire {@code val}.
     * 
     * @param <T>
     * @param name
     * @param array
     * @param val
     * @return 
     * @see ResourceList#acquireOrElse(codex.renthyl.resources.ResourceTicket, java.lang.Object) 
     */
    protected <T> T[] acquireArrayOrElse(String name, T[] array, T val) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquireOrElse(tickets[i], val);
        }
        return array;
    }
    /**
     * Acquires a set of resources from a ticket group and stores them in an
     * array created by the function.
     * <p>
     * Tickets that are invalid will acquire {@code val}.
     * 
     * @param <T>
     * @param name
     * @param func
     * @param val
     * @return 
     * @see ResourceList#acquireOrElse(codex.renthyl.resources.ResourceTicket, java.lang.Object) 
     */
    protected <T> T[] acquireArrayOrElse(String name, Function<Integer, T[]> func, T val) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        T[] array = func.apply(tickets.length);
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquireOrElse(tickets[i], val);
        }
        return array;
    }
    /**
     * Acquires a list of resources from a ticket group and stores them in the given list.
     * 
     * @param <T>
     * @param <R>
     * @param name group name
     * @param collection list to store resources in (or null to create a new {@link LinkedList}).
     * @return given list
     * @see ResourceList#acquire(codex.renthyl.resources.ResourceTicket)
     */
    protected <T, R extends Collection<T>> R acquireList(String name, R collection) {
        Objects.requireNonNull(collection, "Collection to store acquired resources cannot be null.");
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        for (ResourceTicket<T> t : tickets) {
            T res = resources.acquireOrElse(t, null);
            if (res != null) collection.add(res);
        }
        return collection;
    }
    /**
     * Acquires a list of resources from a ticket group and stores them in the given list.
     * 
     * @param <T>
     * @param name group name
     * @return given list
     * @see ResourceList#acquire(codex.renthyl.resources.ResourceTicket)
     */
    protected <T> LinkedList<T> acquireList(String name) {
        return acquireList(name, new LinkedList<>());
    }
    
    /**
     * Releases all reasources associated with any registered ticket.
     * <p>
     * Called automatically after execution if {@link #autoTicketRelease} is true.
     * 
     * @see ResourceList#release(codex.renthyl.resources.ResourceTicket) 
     */
    protected void releaseAll() {
        for (ResourceTicket t : inputs) {
            resources.releaseOptional(t);
        }
        for (ResourceTicket t : outputs) {
            resources.releaseOptional(t);
        }
    }
    
    /**
     * Removes all members of the named group from the input and output lists.
     * 
     * @param <T>
     * @param name
     * @return 
     */
    protected <T> ResourceTicket<T>[] removeGroup(String name) {
        TicketGroup<T> group = groups.remove(name);
        if (group == null) {
            return null;
        }
        // Once we determine which list group members were added to, we only
        // need to remove from that list for future members.
        byte state = 0;
        if (group.isList()) state = 1;
        for (ResourceTicket<T> t : group.getArray()) {
            if (state >= 0 && inputs.remove(t)) {
                state = 1;
            }
            if (state <= 0 && outputs.remove(t)) {
                state = -1;
            }
        }
        return group.getArray();
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
