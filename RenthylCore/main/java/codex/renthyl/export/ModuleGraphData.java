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
package codex.renthyl.export;

import codex.boost.export.SavableObject;
import codex.renthyl.modules.Connectable;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketGroup;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds savable data for a root module and its descendents.
 * 
 * @author codex
 */
public class ModuleGraphData implements Savable {
    
    private static final ArrayList<SavableConnection> DEF_CONNECTIONS = new ArrayList<>(0);
    private static final ArrayList<TicketIndex> DEF_INDICES = new ArrayList<>(0);
    private static int nextModuleId = 0;
    
    private RenderModule rootModule;
    
    public ModuleGraphData() {}
    public ModuleGraphData(RenderModule root) {
        this.rootModule = root;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        if (rootModule == null) {
            throw new NullPointerException("Root module cannot be null.");
        }
        // extract connections
        final ArrayList<SavableConnection> connections = new ArrayList<>();
        final LinkedList<RenderModule> members = new LinkedList<>();
        rootModule.traverse(new ModuleTreeExtractor(members));
        // descend the queue, so that output ids can be reset in the same pass
        final ArrayList<TicketIndex> targetIndices = new ArrayList<>();
        for (Iterator<RenderModule> it = members.descendingIterator(); it.hasNext();) {
            RenderModule m = it.next();
            // generate export indices for input groups
            for (TicketGroup<Object> g : m.getInputGroups().values()) {
                g.generateExportIndices(i -> {
                    i.setModuleId(m.getExportId());
                    i.setGroupName(g.getName());
                    i.setInput(true);
                    targetIndices.add(i);
                });
            }
            // generate export indices for output groups
            for (TicketGroup<Object> g : m.getOutputGroups().values()) {
                g.generateExportIndices(i -> {
                    i.setModuleId(m.getExportId());
                    i.setGroupName(g.getName());
                    i.setInput(false);
                    targetIndices.add(i);
                });
            }
            // remove indices that cannot fetch a source index to connect to
            for (Iterator<TicketIndex> indices = targetIndices.iterator(); indices.hasNext();) {
                TicketIndex i = indices.next();
                if (!i.fetchSourceIndex()) {
                    indices.remove();
                }
            }
            // reset export indices, since they will no longer be used relative to the groups
            for (Iterator<ResourceTicket> tickets = m.ticketIterator(true, true); tickets.hasNext();) {
                tickets.next().clearExportIndex();
            }
        }
        // write
        OutputCapsule out = ex.getCapsule(this);
        out.write(rootModule, "root", null);
        out.writeSavableArrayList(connections, "connections", DEF_CONNECTIONS);
        out.writeSavableArrayList(targetIndices, "targetIndices", DEF_INDICES);
        connections.clear();
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        rootModule = SavableObject.readSavable(in, "root", RenderModule.class, null);
        final ArrayList<TicketIndex> indices = in.readSavableArrayList("targetIndices", DEF_INDICES);
        final HashMap<Integer, Connectable> registry = new HashMap<>();
        final HashMap<String, List<TicketIndex>> indexMap = new HashMap<>();
        rootModule.traverse(m -> { if (registry.put(m.getExportId(), m) != null)
                throw new IllegalStateException("Modules with duplicate ids imported."); });
        // accumulate indices to lists by group
        for (TicketIndex i : indices) {
            String key = i.generateGroupKey();
            List<TicketIndex> c = indexMap.get(key);
            if (c == null) {
                c = new ArrayList<>();
                indexMap.put(key, c);
            }
            c.add(i);
        }
        // apply accumulated indices
        for (List<TicketIndex> c : indexMap.values()) {
            c.get(0).getGroup(registry).applySavedConnections(registry, c);
        }
        indices.clear();
        registry.clear();
    }
    
    public void setRootModule(RenderModule rootModule) {
        this.rootModule = rootModule;
    }
    public RenderModule getRootModule() {
        return rootModule;
    }
    public <T extends RenderModule> T getRootModule(Class<T> requiredType) {
        if (rootModule != null && !requiredType.isAssignableFrom(rootModule.getClass())) {
            throw new ClassCastException("Module tree root is a " + rootModule.getClass().getName()
                    + " when required as a " + requiredType.getName());
        }
        return (T)rootModule;
    }
    
    private static class ModuleTreeExtractor implements Consumer<RenderModule> {
        
        private final LinkedList<RenderModule> members;
        
        public ModuleTreeExtractor(LinkedList<RenderModule> members) {
            this.members = members;
        }
        
        @Override
        public void accept(RenderModule m) {
            m.setExportId(nextModuleId++);
            members.add(m);
        }
        
        public LinkedList<RenderModule> getMembers() {
            return members;
        }
    
    }
    
}
