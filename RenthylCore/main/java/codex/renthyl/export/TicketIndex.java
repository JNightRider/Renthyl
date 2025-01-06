/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.export;

import codex.boost.export.SavableObject;
import codex.renthyl.modules.Connectable;
import codex.renthyl.modules.AbstractRenderModule;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketGroup;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author codex
 */
public abstract class TicketIndex implements Savable {
    
    private int moduleId = -1;
    private String groupName;
    private boolean input = true;
    private ResourceTicket ticket;
    private TicketIndex source;
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        assert moduleId >= 0 : "Connectable ID not specified.";
        Objects.requireNonNull(groupName, "Group name not specified.");
        out.write(moduleId, "moduleId", 0);
        out.write(groupName, "groupName", AbstractRenderModule.MAIN_GROUP);
        out.write(source, "source", null);
        write(out);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        moduleId = in.readInt("moduleId", 0);
        groupName = in.readString("groupName", AbstractRenderModule.MAIN_GROUP);
        source = SavableObject.readSavable(in, "source", TicketIndex.class, null);
        read(in);
    }
    
    protected abstract void write(OutputCapsule out) throws IOException;
    protected abstract void read(InputCapsule in) throws IOException;
    public abstract TicketSelector createSelector();
    
    public Connectable getConnectable(Map<Integer, Connectable> registry) {
        return Objects.requireNonNull(registry.get(moduleId), "No Connectable registered under " + moduleId);
    }
    public TicketGroup getGroup(Map<Integer, Connectable> registry) {
        Connectable c = getConnectable(registry);
        return Objects.requireNonNull(c.getGroup(groupName, input),
                "No group under \"" + groupName + "\" as an " + (input ? "input" : "output") + " group.");
    }
    
    public <T extends TicketIndex> T cast(Class<T> type) {
        if (!type.isAssignableFrom(getClass())) {
            throw new ClassCastException("Expected type " + type.getName() + " but found " + getClass().getName());
        }
        return (T)this;
    }
    public boolean fetchSourceIndex() {
        return ticket != null && ticket.getSource() != null
                && (source = ticket.getSource().getExportIndex()) != null;
    }
    public String generateGroupKey() {
        return moduleId + "/" + groupName + "/" + input;
    }

    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public void setInput(boolean input) {
        this.input = input;
    }
    public void setTicket(ResourceTicket ticket) {
        this.ticket = ticket;
    }

    public int getModuleId() {
        return moduleId;
    }
    public String getGroupName() {
        return groupName;
    }
    public boolean isInput() {
        return input;
    }
    public ResourceTicket getTicket() {
        return ticket;
    }
    public TicketIndex getSource() {
        return source;
    }
    
}
