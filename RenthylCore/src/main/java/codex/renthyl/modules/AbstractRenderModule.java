/*
 * Copyright (c) 2025, codex
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
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketList;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import codex.renthyl.resources.tickets.TicketGroup;

/**
 *
 * @author codex
 */
public abstract class AbstractRenderModule implements RenderModule {
    
    public static final String MAIN_GROUP = "MAIN_GROUP";
    
    protected FrameGraph frameGraph;
    protected String name;
    protected RenderContainer parent;
    protected final ModuleIndex index = new ModuleIndex();
    protected final HashMap<String, TicketGroup> inputGroups = new HashMap<>();
    protected final HashMap<String, TicketGroup> outputGroups = new HashMap<>();
    private int refs = 1; // start at one so this won't be temporally culled
    private int exportId = -1;
    
    public AbstractRenderModule() {
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
        out.write(exportId, "exportId", -1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "RenderModule");
        exportId = in.readInt("exportId", -1);
    }
    
    @Override
    public ResourceTicket addInput(String name) {
        return getMainInputGroup().add(name);
    }
    @Override
    public ResourceTicket addOutput(String name) {
        return getMainOutputGroup().add(name);
    }
    @Override
    public <T, R extends TicketGroup<T>> R addInputGroup(R group) {
        if (inputGroups.put(group.getName(), group) != null) {
            throw new IllegalArgumentException("Group already registered under \"" + group.getName() + "\" for " + this);
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
    
    @Override
    public void initializeModule(FrameGraph frameGraph) {
        if (this.frameGraph != null) {
            throw new IllegalStateException("Module already initialized.");
        }
        if (name == null) {
            name = getClass().getSimpleName();
        }
        this.frameGraph = frameGraph;
        this.frameGraph.setLayoutUpdateNeeded();
        createMainGroups();
        //initModule(this.frameGraph);
    }
    @Override
    public void updateModule(FGRenderContext context, float tpf) {}
    @Override
    public void queueModule(FGRenderContext context, ExecutionJobList queues, int parentThread) {
        index.set(queues.add(this, parentThread));
    }
    @Override
    public void prepareRender(FGRenderContext context) {}
    @Override
    public void executeRender(FGRenderContext context) {}
    @Override
    public void resetRender(FGRenderContext context) {}
    @Override
    public void cleanupModule() {
        if (frameGraph != null) {
            frameGraph.setLayoutUpdateNeeded();
            //cleanupModule(frameGraph);
            for (TicketGroup g : inputGroups.values()) {
                g.detach();
            }
            for (TicketGroup g : outputGroups.values()) {
                g.detach();
            }
            outputGroups.clear();
            inputGroups.clear();
            frameGraph = null;
        }
    }
    
    @Override
    public void setExportId(int id) {
        this.exportId = id;
    }
    @Override
    public int getExportId() {
        return exportId;
    }
    
    /**
     * Adds an input and an output group to the module to be main groups.
     */
    private void createMainGroups() {
        if (!addInputGroup(createMainInputGroup(MAIN_GROUP)).getName().equals(MAIN_GROUP)) {
            throw new IllegalStateException("Main input group's name must be \"" + MAIN_GROUP + "\"");
        }
        if (!addOutputGroup(createMainOutputGroup(MAIN_GROUP)).getName().equals(MAIN_GROUP)) {
            throw new IllegalStateException("Main output group's name must be \"" + MAIN_GROUP + "\"");
        }
    }
    /**
     * Creates a group to be used as the main input group.
     * <p>
     * Implementation is required to use {@code name} as the
     * name of the ticket group.
     * 
     * @param name
     * @return 
     */
    protected TicketGroup createMainInputGroup(String name) {
        return new TicketList(name);
    }
    /**
     * Creates a group to be used as the main output group.
     * <p>
     * Implementation is required to use {@code name} as the
     * name of the ticket group.
     * 
     * @param name
     * @return 
     */
    protected TicketGroup createMainOutputGroup(String name) {
        return new TicketList(name);
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
     * Sets the parent of this module.
     * 
     * @param parent
     * @return 
     */
    @Override
    public boolean setParent(RenderContainer parent) {
        this.parent = parent;
        return true;
    }
    
    /**
     * 
     * @return 
     */
    @Override
    public String getName() {
        return name;
    }
    /**
     * 
     * @return 
     */
    @Override
    public RenderContainer getParent() {
        return parent;
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
