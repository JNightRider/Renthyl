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
import codex.renthyl.resources.ResourceUser;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketList;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import codex.renthyl.resources.tickets.TicketGroup;

/**
 *
 * @author codex
 */
public abstract class RenderModule implements NewConnectable, ResourceUser, Savable {
    
    protected static final String MAIN_GROUP = "MAIN_GROUP";
    
    protected FrameGraph frameGraph;
    protected String name;
    protected RenderContainer parent;
    protected final ModuleIndex index = new ModuleIndex();
    protected final HashMap<String, TicketGroup> inputGroups = new HashMap<>();
    protected final HashMap<String, TicketGroup> outputGroups = new HashMap<>();
    private BiConsumer<RenderContainer, RenderModule> connector;
    private int refs = 1; // start at one so this won't be temporally culled
    private int id = -1;
    
    public RenderModule() {
    }
    
    @Override
    public Iterator<ResourceTicket> getInputTickets() {
        return ticketIterator(true, false);
    }
    @Override
    public Iterator<ResourceTicket> getOutputTickets() {
        return ticketIterator(false, true);
    }
    @Override
    public ModuleIndex getIndex() {
        return index;
    }
    @Override
    public void countReferences() {
        refs = 0;
        for (TicketGroup c : outputGroups.values()) {
            refs += c.size();
        }
    }
    @Override
    public void dereference() {
        if (--refs < 0) {
            throw new IllegalStateException("Cannot dereference unreferenced module.");
        }
    }
    @Override
    public boolean isUsed() {
        return refs > 0;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", "RenderModule");
        out.write(id, "exportId", -1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "RenderModule");
        id = in.readInt("exportId", -1);
    }
    
    @Override
    public <T> ResourceTicket<T> addInput(ResourceTicket ticket) {
        getMainInputGroup().add(ticket);
        return ticket;
    }
    @Override
    public <T> ResourceTicket<T> addOutput(ResourceTicket ticket) {
        getMainOutputGroup().add(ticket);
        return ticket;
    }
    @Override
    public <T, R extends TicketGroup<T>> R addInputGroup(R group) {
        if (inputGroups.put(group.getName(), group) != null) {
            throw new IllegalArgumentException("Group already registered under \"" + group.getName() + "\".");
        }
        group.attach(this);
        return group;
    }
    @Override
    public <T, R extends TicketGroup<T>> R addOutputGroup(R group) {
        if (outputGroups.put(group.getName(), group) != null) {
            throw new IllegalArgumentException("Group already registered under \"" + group.getName() + "\".");
        }
        group.attach(this);
        return group;
    }
    @Override
    public HashMap<String, TicketGroup> getInputGroups() {
        return inputGroups;
    }
    @Override
    public HashMap<String, TicketGroup> getOutputGroups() {
        return outputGroups;
    }
    @Override
    public TicketGroup<Object> getMainInputGroup() {
        return inputGroups.get(MAIN_GROUP);
    }
    @Override
    public TicketGroup<Object> getMainOutputGroup() {
        return outputGroups.get(MAIN_GROUP);
    }
    @Override
    public void setLayoutUpdateNeeded() {
        if (frameGraph != null) {
            frameGraph.setLayoutUpdateNeeded();
        }
    }
    
    public int getId() {
        return id;
    }
    
    /**
     * Initializes this module to the FrameGraph.
     * 
     * @param frameGraph 
     * @throws IllegalStateException if module is already initialized to a FrameGraph.
     */
    public void initializeModule(FrameGraph frameGraph) {
        if (this.frameGraph != null) {
            throw new IllegalStateException("Module already initialized.");
        }
        if (name == null) {
            name = getClass().getSimpleName();
        }
        this.frameGraph = frameGraph;
        this.frameGraph.setLayoutUpdateNeeded();
        id = this.frameGraph.getNextId();
        createMainGroups();
        initModule(this.frameGraph);
    }
    /**
     * Updates the module before any rendering-related operations occur.
     * <p>
     * This gives modules a chance to determine if the layout needs changes.
     * 
     * @param context
     * @param tpf 
     */
    public void updateModule(FGRenderContext context, float tpf) {
        for (TicketGroup g : inputGroups.values()) {
            g.update();
        }
        for (TicketGroup g : outputGroups.values()) {
            g.update();
        }
    }
    /**
     * Updates this module's index from the supplier.
     * 
     * @param context
     * @param queues
     * @param parentThread
     */
    public void queueModule(FGRenderContext context, ExecutionJobList queues, int parentThread) {
        index.set(queues.add(this, parentThread));
    }
    /**
     * Executes this module.
     * 
     * @param context 
     */
    public void executeModuleRender(FGRenderContext context) {
        if (!isUsed()) {
            return;
        }
        executeRender(context);
    }
    /**
     * Resets this module from execution.
     * 
     * @param context 
     */
    public void resetModuleRender(FGRenderContext context) {
        resetRender(context);
    }
    /**
     * Cleans up this module from being attached to a FrameGraph.
     */
    public void cleanupModule() {
        for (TicketGroup g : inputGroups.values()) {
            g.detach();
        }
        for (TicketGroup g : outputGroups.values()) {
            g.detach();
        }
        id = -1;
        if (frameGraph != null) {
            frameGraph.setLayoutUpdateNeeded();
            cleanupModule(frameGraph);
            for (TicketGroup g : inputGroups.values()) {
                g.disconnect();
            }
            for (TicketGroup g : outputGroups.values()) {
                g.disconnect();
            }
            outputGroups.clear();
            inputGroups.clear();
            frameGraph = null;
        }
    }
    
    /**
     * Adds an input and an output group to the module to be main groups.
     */
    protected void createMainGroups() {
        addInputGroup(new TicketList(MAIN_GROUP));
        addOutputGroup(new TicketList(MAIN_GROUP));
    }
    
    /**
     * Initializes the RenderModule implementation.
     * <p>
     * For most cases, use {@link #initializeModule(com.jme3.renderer.framegraph.FrameGraph)}
     * instead.
     * 
     * @param frameGraph 
     */
    protected abstract void initModule(FrameGraph frameGraph);
    /**
     * Prepares the RenderModule implementation.
     * <p>
     * For most cases, use
     * {@link #prepareModuleRender(com.jme3.renderer.framegraph.FGRenderContext, com.jme3.renderer.framegraph.PassIndex)}
     * instead.
     * 
     * @param context 
     */
    protected abstract void prepareModuleRender(FGRenderContext context);
    /**
     * Executes the RenderModule implementation.
     * <p>
     * For most cases, use {@link #executeModuleRender(com.jme3.renderer.framegraph.FGRenderContext)}
     * instead.
     * 
     * @param context 
     */
    protected abstract void executeRender(FGRenderContext context);
    /**
     * Resets the RenderModule after execution.
     * 
     * @param context 
     */
    protected abstract void resetRender(FGRenderContext context);
    /**
     * Cleans up the RenderModule implementation.
     * <p>
     * For most cases, use {@link #cleanupModule()} instead.
     * 
     * @param frameGraph 
     */
    protected abstract void cleanupModule(FrameGraph frameGraph);
    
    /**
     * Called when all rendering is complete in a render frame this
     * module participated in (regardless of culling).
     */
    public abstract void renderingComplete();
    /**
     * Traverses this module.
     * 
     * @param traverser 
     */
    public abstract void traverse(Consumer<RenderModule> traverser);
    
    /**
     * 
     * @param container 
     */
    public void applyConnector(RenderContainer container) {
        if (connector != null) {
            connector.accept(container, this);
        }
    }
    
    /**
     * Sets the name of this module.
     * 
     * @param name 
     */
    public void setName(String name) {
        assert name != null : "Name cannot be null.";
        assert !name.isEmpty() : "Name cannot be an empty string.";
        this.name = name;
    }
    /**
     * Sets the name of this module.
     * 
     * @param name
     * @param preserveProjectName if true, the project name appended to the
     * current name (if any) will be preserved into the resulting name.
     */
    public void setName(String name, boolean preserveProjectName) {
        if (preserveProjectName) {
            String[] array = this.name.split(":", 2);
            if (array.length > 1) {
                setName(array[0]+":"+name);
                return;
            }
        }
        setName(name);
    }
    /**
     * Appends the project name to the beginning of the current name.
     * <p>
     * The resulting name is formatted as {@code "[projectName]:[currentName]"}.
     * If the current name already has a project name appended (denoted by ':'),
     * the given project name will override that.
     * <p>
     * This can be used to ensure that modules between "projects" included in
     * the framegraph do not have duplicate names. For example, if importing
     * a bloom effect project, set the project name to "Bloom" so that modules
     * in the project can be referenced without fear of accidentally referencing
     * something else.
     * 
     * @param projectName 
     */
    public void appendProjectName(String projectName) {
        String[] array = name.split(":", 2);
        if (array.length == 1) {
            setName(projectName+":"+name);
        } else {
            setName(projectName+":"+array[1]);
        }
    }
    /**
     * 
     * @param connector 
     */
    public void setConnector(BiConsumer<RenderContainer, RenderModule> connector) {
        this.connector = connector;
    }
    /**
     * Sets the parent of this module.
     * 
     * @param parent
     * @return 
     */
    protected boolean setParent(RenderContainer parent) {
        this.parent = parent;
        return true;
    }
    
    /**
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    /**
     * Gets the project name appended to the current name, if any.
     * 
     * @return project name, or null
     */
    public String getProjectName() {
        String[] array = name.split(":");
        if (array.length > 1) {
            return array[0];
        } else {
            return null;
        }
    }
    /**
     * 
     * @return 
     */
    public RenderContainer getParent() {
        return parent;
    }
    /**
     * 
     * @return 
     */
    public BiConsumer<RenderContainer, RenderModule> getConnector() {
        return connector;
    }
    /**
     * Returns true if this module is assigned to a FrameGraph.
     * 
     * @return 
     */
    public boolean isAssigned() {
        return frameGraph != null;
    }
    /**
     * Returns true if this module runs on a thread other than the main thread.
     * 
     * @return 
     */
    public boolean isAsync() {
        return !index.isMainThread();
    }
    
}
