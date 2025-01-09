/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import codex.renthyl.modules.AbstractRenderModule;
import codex.renthyl.modules.Connectable;
import java.util.Collection;

/**
 *
 * @author codex
 * @param <T>
 */
public class TicketSignature <T extends TicketSelector> implements TicketSelector {
    
    public static final TicketSignature NULL = new TicketSignature(true, TicketSelector.None);
    
    private String groupName;
    private boolean input;
    private T selector;
    
    public TicketSignature(boolean input, T selector) {
        this(AbstractRenderModule.MAIN_GROUP, input, selector);
    }
    public TicketSignature(String groupName, boolean input, T selector) {
        this.groupName = groupName;
        this.input = input;
        this.selector = selector;
    }
    
    @Override
    public boolean select(ResourceTicket ticket, ResourceTicket other, int index) {
        return selector.select(ticket, other, index);
    }
    
    public void selectFrom(Connectable connectable, Collection<ResourceTicket<Object>> selected) {
        selector.selectFrom(getGroupOf(connectable), selected);
    }
    public void selectFrom(Connectable connectable, Collection<ResourceTicket<Object>> selected, ResourceTicket orElse) {
        selector.selectFrom(getGroupOf(connectable), selected, orElse);
    }
    
    public TicketGroup getGroupOf(Connectable connectable) {
        if (groupName != null) {
            return connectable.getGroup(groupName, input);
        } else if (input) {
            return connectable.getMainInputGroup();
        } else {
            return connectable.getMainOutputGroup();
        }
    }

    public String getGroupName() {
        return groupName;
    }
    public boolean isInput() {
        return input;
    }
    public T getSelector() {
        return selector;
    }
    
}
