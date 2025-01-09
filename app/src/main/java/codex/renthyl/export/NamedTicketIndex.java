/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.export;

import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.Objects;

/**
 *
 * @author codex
 */
public class NamedTicketIndex extends TicketIndex {
    
    private String ticketName;
    
    @Override
    protected void write(OutputCapsule out) throws IOException {
        Objects.requireNonNull(ticketName, "Ticket name not specified.");
        out.write(ticketName, "ticketName", "Result"); // "Result" is a very common ticket name
    }
    @Override
    protected void read(InputCapsule in) throws IOException {
        ticketName = in.readString("ticketName", "Result");
    }
    @Override
    public TicketSelector createSelector() {
        return TicketSelector.name(ticketName);
    }
    
    public void setTicketName(String ticketName) {
        this.ticketName = ticketName;
    }
    public String getTicketName() {
        return ticketName;
    }
    
}
