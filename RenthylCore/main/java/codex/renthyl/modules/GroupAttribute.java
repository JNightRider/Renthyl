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

import codex.boost.export.SavableObject;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.client.GraphTarget;
import codex.renthyl.resources.tickets.ArbitraryTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketArray;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.ViewPort;
import java.io.IOException;
import java.util.ArrayList;
import codex.renthyl.resources.tickets.TicketGroup;

/**
 * Accepts and produces and group of inputs and outputs to/from game logic.
 * <p>
 * The size of each group is determine by the specified group size. Each given
 * {@link GraphSource} and {@link GraphTarget} map index-to-index to an individual
 * output or input. Surplus sources and targets are not used.
 * <p>
 * Inputs:
 * <ul>
 *   <li>{@link #INPUT}[n] ({@link Object}): input group of a specified size (optional).
 * </ul>
 * Outputs:
 * <ul>
 *   <li>{@link #OUTPUT}[n] ({@link Object)}: output group of a specified size.
 * </ul>
 * 
 * @author codex
 */
public class GroupAttribute extends RenderPass {
    
    public static final String INPUT = "Input", OUTPUT = "Output";
    
    private int groupSize = 2;
    private ArbitraryTicketList<Object> input;
    private TicketArray<Object> output;
    private final ArrayList<GraphSource> sources = new ArrayList<>(5);
    private final ArrayList<GraphTarget> targets = new ArrayList<>(5);
    
    public GroupAttribute() {}
    public GroupAttribute(int groupSize) {
        this.groupSize = groupSize;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        input = addInputGroup(new ArbitraryTicketList(INPUT));
        output = addOutputGroup(new TicketArray(OUTPUT, groupSize));
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket t : output) {
            declare(null, t);
        }
        referenceOptional(input);
    }
    @Override
    protected void execute(FGRenderContext context) {
        ViewPort vp = context.getViewPort();
        int i = 0;
        for (ResourceTicket t : input.getTickets()) {
            if (i >= targets.size()) {
                break;
            }
            Object value = resources.acquireOrElse(t, null);
            GraphTarget target = targets.get(i++);
            if (target != null && target.setGraphValue(frameGraph, vp, value)) {
                resources.setConstant(t);
            }
        }
        i = 0;
        for (ResourceTicket t : output) {
            if (i < sources.size()) {
                GraphSource s = sources.get(i++);
                if (s != null) {
                    Object value = s.getGraphValue(frameGraph, vp);
                    if (value != null) {
                        resources.setPrimitive(t, value);
                        continue;
                    }
                }
            }
            resources.setUndefined(t);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(groupSize, "groupSize", 2);
        SavableObject.writeFromCollection(out, sources, "sources");
        SavableObject.writeFromCollection(out, targets, "targets");
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        groupSize = in.readInt("groupSize", 2);
        SavableObject.readToCollection(in, "sources", sources);
        SavableObject.readToCollection(in, "targets", targets);
    }
    
    /**
     * Sets the size of the input and output groups.
     * 
     * @param groupSize 
     * @throws IllegalStateException if called while pass is assigned to a framegraph
     */
    public void setGroupSize(int groupSize) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot alter group size while assigned to a framegraph.");
        }
        this.groupSize = groupSize;
    }
    /**
     * Sets the source that provides values for the output at the index
     * within the output group.
     * 
     * @param i
     * @param source 
     */
    public void setSource(int i, GraphSource source) {
        while (sources.size() <= i) {
            sources.add(null);
        }
        sources.set(i, source);
    }
    /**
     * Sets the target that recieves values from the input at the index
     * within the input group.
     * 
     * @param i
     * @param target 
     */
    public void setTarget(int i, GraphTarget target) {
        while (targets.size() <= i) {
            targets.add(null);
        }
        targets.set(i, target);
    }
    
    /**
     * 
     * @return 
     */
    public int getGroupSize() {
        return groupSize;
    }
    /**
     * Gets the source at the index.
     * 
     * @param i
     * @return source at the index, or null if no source is assigned at the index
     */
    public GraphSource getSource(int i) {
        if (i < sources.size()) {
            return sources.get(i);
        } else {
            return null;
        }
    }
    /**
     * Gets the target at the index.
     * 
     * @param i
     * @return target at the index, or null if no target is assigned at the index.
     */
    public GraphTarget getTarget(int i) {
        if (i < targets.size()) {
            return targets.get(i);
        } else {
            return null;
        }
    }
    
    /**
     * Gets the name of the input at the index.
     * 
     * @param i
     * @return 
     */
    public static String getInput(int i) {
        return INPUT+'['+i+']';
    }
    /**
     * Gets the name of the output at the index.
     * 
     * @param i
     * @return 
     */
    public static String getOutput(int i) {
        return OUTPUT+'['+i+']';
    }
    
}
