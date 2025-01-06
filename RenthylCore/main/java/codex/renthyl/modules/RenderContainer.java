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

import codex.renthyl.jobs.ExecutionJobList;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contains a list of child modules which are executed when
 * this container is executed.
 * 
 * @author codex
 * @param <R>
 */
public class RenderContainer <R extends RenderModule> extends AbstractRenderModule implements Iterable<R> {

    protected final ArrayList<R> queue = new ArrayList<>();
    protected Consumer<R> moduleInitializer;
    
    public RenderContainer() {}
    public RenderContainer(String name) {
        setName(name);
    }
    
    @Override
    public void initializeModule(FrameGraph frameGraph) {
        super.initializeModule(frameGraph);
        for (RenderModule m : queue) {
            m.initializeModule(frameGraph);
        }
    }
    @Override
    public void cleanupModule() {
        for (RenderModule m : queue) {
            m.cleanupModule();
        }
        super.initializeModule(frameGraph);
    }
    @Override
    public void updateModule(FGRenderContext context, float tpf) {
        super.updateModule(context, tpf);
        for (R m : queue) {
            m.updateModule(context, tpf);
        }
    }
    @Override
    public void queueModule(FGRenderContext context, ExecutionJobList queues, int parentThread) {
        index.set(queues.add(this, parentThread));
        for (RenderModule m : queue) {
            m.queueModule(context, queues, parentThread);
        }
    }
    @Override
    public void prepareModuleRender(FGRenderContext context) {
        super.prepareModuleRender(context);
        for (RenderModule m : queue) {
            // Checking for usage and culling states is critical. Niavely preparing modules
            // could lead to resources not fully being released which will result in an exception.
            if (!context.isTemporalCulling() || context.getFrameGraph().isLayoutUpdateNeeded() || m.isUsed()) {
                m.prepareModuleRender(context);
            }
        }
    }
    @Override
    public void resetRender(FGRenderContext context) {
        for (RenderModule m : queue) {
            m.resetRender(context);
        }
    }
    @Override
    public void renderingComplete() {
        for (RenderModule m : queue) {
            m.renderingComplete();
        }
    }
    @Override
    public void countReferences() {
        for (RenderModule m : queue) {
            m.countReferences();
        }
    }
    @Override
    public boolean isUsed() {
        // If executing a container becomes heavy on its own, change this to
        // check isUsed() for each contained module.
        return !queue.isEmpty();
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        ArrayList<RenderModule> array = new ArrayList<>();
        array.addAll(queue);
        out.writeSavableArrayList(array, "queue", new ArrayList<>(0));
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        ArrayList<R> array = in.readSavableArrayList("queue", new ArrayList<>(0));
        queue.addAll(array);
    }
    @Override
    public Iterator<R> iterator() {
        return queue.iterator();
    }
    @Override
    public void traverse(Consumer<RenderModule> traverser) {
        traverser.accept(this);
        for (RenderModule m : queue) {
            m.traverse(traverser);
        }
    }
    
    /**
     * Adds the module at the index.
     * <p>
     * If {@code index} is negative, the module will be appended
     * to the end of this container's queue.
     * 
     * @param <T>
     * @param module
     * @param index
     * @return the added module
     */
    public <T extends R> T add(T module, int index) {
        Objects.requireNonNull(module, "Cannot add null module.");
        if (this == module) {
            throw new IllegalArgumentException("Cannot add container to itself.");
        }
        if (module.getParent() != null) {
            module.getParent().remove(module);
        }
        if (index < 0) {
            index = queue.size();
        }
        if (module.setParent(this)) {
            queue.add(index, module);
            if (isAssigned()) {
                module.initializeModule(frameGraph);
            }
            if (moduleInitializer != null) {
                moduleInitializer.accept(module);
            }
            return module;
        }
        throw new IllegalArgumentException(module + " cannot be added to " + this + ".");
    }
    
    /**
     * Adds the module to the end of this container.
     * 
     * @param <T>
     * @param module
     * @return 
     */
    public <T extends R> T add(T module) {
        return add(module, queue.size());
    }
    
    /**
     * Adds a sequence of modules where the first module occupies the
     * specified index.
     * <p>
     * The array contains the modules to add in order. If any element is null,
     * the Function is used to create a new module, which then replaces the
     * null element.
     * <p>
     * An input ticket (specified by {@code target}) of each module (except the first)
     * is connected to an output ticket (specified by {@code source}) of the
     * previous module in the sequence.
     * 
     * @param <T>
     * @param array array of modules to add in sequence (null elements are replaced
     * using the Function)
     * @param start index the first module in the sequence will occupy.
     * @param factory creates replacements for null elements (may be null if
     * no elements are null)
     * @param source name of the output ticket that gets connected to the next module
     * in the sequence
     * @param target name of the input ticket that gets connected to the previous module
     * in the sequence
     * @return the given array of modules, with null elements replaced using the Function
     */
    public <T extends R> T[] addLoop(T[] array, int start, Function<Integer, T> factory, String source, String target) {
        for (int i = 0; i < array.length; i++) {
            T module = array[i];
            if (module == null) {
                if (factory == null) {
                    throw new NullPointerException("Module to add cannot be null.");
                }
                module = array[i] = factory.apply(i);
            }
            if (start >= 0) {
                add(module, start++);
            } else {
                add(module);
            }
            if (i > 0) {
                module.makeInput(array[i-1], source, target);
            }
        }
        return array;
    }
    
    /**
     * Gets the module at the index.
     * 
     * @param index
     * @return 
     */
    public R get(int index) {
        return queue.get(index);
    }
    
    /**
     * Gets a module under this container (but not necessary a direct child of)
     * that satisfies the locator.
     * 
     * @param <T>
     * @param by module locator (not null)
     * @return located module, or null if none is found
     */
    public <T extends RenderModule> T get(ModuleLocator<T> by) {
        for (RenderModule m : queue) {
            T module = by.accept(m);
            if (module != null) {
                return module;
            } else if (m instanceof RenderContainer) {
                module = (T)((RenderContainer)m).get(by);
                if (module != null) {
                    return module;
                }
            }
        }
        return null;
    }
    
    /**
     * Removes the module from this container.
     * 
     * @param module
     * @return true only if the module was removed
     */
    public boolean remove(R module) {
        if (module.getParent() == this && queue.remove(module)) {
            module.setParent(null);
            module.cleanupModule();
            return true;
        }
        return false;
    }
    
    /**
     * Removes the module at the index.
     * 
     * @param index
     * @return removed module at the index
     */
    public R remove(int index) {
        if (index < 0 || index >= queue.size()) {
            return null;
        }
        R m = queue.remove(index);
        m.setParent(null);
        m.cleanupModule();
        return m;
    }
    
    /**
     * Returns the index of the module in this container.
     * 
     * @param module
     * @return index, or -1 if module does not belong to this container
     */
    public int indexOf(R module) {
        return queue.indexOf(module);
    }
    
    /**
     * Clears all child modules from this container.
     */
    public void clear() {
        for (RenderModule m : queue) {
            m.cleanupModule();
        }
        queue.clear();
    }
    
    /**
     * Gets the number of child modules in this container.
     * 
     * @return 
     */
    public int size() {
        return queue.size();
    }
    
    /**
     * Connects the named source (input) ticket from this container to the named
     * target (input) ticket from the target child Connectable.
     * 
     * @param sourceTicket
     * @param targetTicket
     * @param target 
     */
    public void makeInternalInput(String sourceTicket, String targetTicket, Connectable target) {
        makeInternalInput(TicketSelector.name(sourceTicket), TicketSelector.name(targetTicket), target);
    }
    
    /**
     * Connects selected source (input) tickets from this container to the selected
     * target (input) tickets from the target child Connectable.
     * 
     * @param sourceSelector
     * @param targetSelector
     * @param target 
     */
    public void makeInternalInput(TicketSelector sourceSelector, TicketSelector targetSelector, Connectable target) {
        target.getMainInputGroup().makeInput(getMainInputGroup(), sourceSelector, targetSelector);
    }
    
    /**
     * Connects the named source (output) ticket from the source connectable
     * to the named target (output) ticket from this container.
     * 
     * @param source
     * @param sourceTicket
     * @param targetTicket 
     */
    public void makeInternalOutput(Connectable source, String sourceTicket, String targetTicket) {
        makeInternalOutput(source, TicketSelector.name(sourceTicket), TicketSelector.name(targetTicket));
    }
    
    /**
     * Connects the selected source (output) tickets from the source connectable
     * to the selected target (output) tickets from this container.
     * 
     * @param source
     * @param sourceSelector
     * @param targetSelector 
     */
    public void makeInternalOutput(Connectable source, TicketSelector sourceSelector, TicketSelector targetSelector) {
        getMainOutputGroup().makeInput(source.getMainOutputGroup(), sourceSelector, targetSelector);
    }
    
    /**
     * 
     * @param moduleInitializer 
     */
    public void setModuleInitializer(Consumer<R> moduleInitializer) {
        this.moduleInitializer = moduleInitializer;
    }
    
    /**
     * 
     * @return 
     */
    public Consumer<R> getModuleInitializer() {
        return moduleInitializer;
    }
    
}
