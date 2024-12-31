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
package codex.renthyl.resources;

import codex.renthyl.Connectable;
import codex.renthyl.resources.tickets.TicketCollection;
import java.util.Collection;
import java.util.LinkedList;

/**
 * References a {@link RenderResource} by index.
 * <p>
 * Can reference another ticket as a source, which makes this point to the same
 * resource as the source ticket. This mechanism allows RenderPasses to share
 * resources. Also vaguely tracks the last seen render object, which is used to
 * prioritize that render object, especially for reservations.
 * 
 * @author codex
 * @param <T>
 */
public class ResourceTicket <T> {
    
    public static final String RESERVED = "#";
    public static final int INVALID = -1, FORCE_INVALID = -2;
    
    private TicketCollection<T> group;
    private String name;
    private int localIndex;
    private int exportGroupId = -1;
    private long objectId = -1;
    private boolean bound = false;
    private boolean overrideWorld = false;
    private ResourceTicket<T> source;
    private final LinkedList<ResourceTicket<T>> targets = new LinkedList<>();
    
    public ResourceTicket() {
        this(null, INVALID);
    }
    public ResourceTicket(String name) {
        this(name, INVALID);
    }
    public ResourceTicket(String name, int index) {
        this.name = name;
        this.localIndex = index;
    }
    
    /**
     * Sets the flag indicating that this ticket is currently bound to a ResourceView.
     * That is, this ticket has been used to declare or reference a ResourceView,
     * but has not yet released.
     * <p>
     * Called internally. <strong>Do not use.</strong>
     */
    public void setBindFlag() {
        if (bound) {
            throw new IllegalStateException("Cannot make multiple declarations/references with a single ticket: " + this);
        }
        bound = true;
    }
    /**
     * Clears the flag indicating that this ticket is currently bound to a ResourceView.
     * <p>
     * Called internally. <strong>Do not use.</strong>
     * 
     * @see #setBindFlag() 
     */
    public void clearBindFlag() {
        bound = false;
    }
    
    /**
     * Clears all target tickets.
     */
    public void clearAllTargets() {
        for (ResourceTicket<T> t : targets) {
            t.source = null;
        }
        targets.clear();
    }
    
    /**
     * Copies this ticket's resource index to the target ticket.
     * 
     * @param target
     * @return 
     */
    public ResourceTicket<T> copyIndexTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        return target.setLocalIndex(localIndex);
    }
    /**
     * Copies this ticket's object ID to the target ticket.
     * 
     * @param target
     * @return 
     */
    public ResourceTicket<T> copyObjectTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        target.setObjectId(objectId);
        return target;
    }
    
    /**
     * 
     * @param group 
     */
    protected void setGroup(TicketCollection<T> group) {
        this.group = group;
    }
    /**
     * Sets the source ticket.
     * 
     * @param source 
     */
    public void setSource(ResourceTicket<T> source) {
        if (this.source != source) {
            if (this.source != null) {
                this.source.targets.remove(this);
            }
            if (group != null) {
                group.setLayoutUpdateNeeded();
            }
            this.source = source;
            if (this.source != null) {
                this.source.targets.add(this);
            }
        }
    }
    /**
     * Sets the name of this ticket.
     * 
     * @param name
     * @return 
     */
    public ResourceTicket<T> setName(String name) {
        this.name = name;
        return this;
    }
    /**
     * Sets the local index.
     * <p>
     * The local index is overriden if the source ticket is not null and
     * the source's world index is not negative.
     * 
     * @param index
     * @return 
     */
    protected ResourceTicket<T> setLocalIndex(int index) {
        this.localIndex = index;
        return this;
    }
    /**
     * Sets this ticket to override the inherited world index with its
     * own local index.
     * <p>
     * This is usually used by "referencing" tickets to temporarily ignore
     * an incoming resource.
     * <p>
     * default={@code false}
     * 
     * @param override 
     */
    public void setOverrideWorldIndex(boolean override) {
        if (source != null && this.overrideWorld != override && group != null) {
            group.setLayoutUpdateNeeded();
        }
        this.overrideWorld = override;
    }
    /**
     * Sets the object ID.
     * 
     * @param objectId 
     */
    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }
    /**
     * Sets the id of the group this ticket is exported with.
     * <p>
     * Called internally. Do not use.
     * 
     * @param exportGroupId 
     */
    public void setExportGroupId(int exportGroupId) {
        this.exportGroupId = exportGroupId;
    }
    
    /**
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    /**
     * Gets the world index.
     * <p>
     * World index for this ticket is inherited from this ticket's source. If the
     * source ticket is null or its world index returns negative, this ticket's
     * {@link #getLocalIndex() local index} will be returned instead.
     * <p>
     * The index, if not negative, corresponds to a {@link ResourceView}
     * held by a {@link ResourceList}.
     * 
     * @return world index
     */
    public int getWorldIndex() {
        if (source != null && !overrideWorld) {
            int i = source.getWorldIndex();
            if (i >= 0) return i;
        }
        return localIndex;
    }
    /**
     * Returns the local index.
     * <p>
     * The index, if not negative, corresponds to a {@link ResourceView}
     * held by a {@link ResourceList}.
     * 
     * @return local index
     */
    public int getLocalIndex() {
        return localIndex;
    }
    /**
     * The ID of the last object this ticket was associated with.
     * 
     * @return object ID, or -1 if this ticket was never associated with an object
     */
    public long getObjectId() {
        return objectId;
    }
    /**
     * 
     * @return 
     */
    public ResourceTicket<T> getSource() {
        return source;
    }
    /**
     * Returns true if this ticket is currently associated with a ResourceView.
     * That is, if this ticket was used to declare or reference a ResourceView
     * and has not yet released.
     * 
     * @return 
     */
    public boolean isBindFlagSet() {
        return bound;
    }
    /**
     * Returns true if this source ticket is not null.
     * 
     * @return 
     */
    public boolean hasSource() {
        return source != null;
    }
    /**
     * 
     * @return 
     */
    public boolean isOverrideWorldIndex() {
        return overrideWorld;
    }
    /**
     * Gets all tickets depending on this ticket.
     * 
     * @return 
     */
    public Collection<ResourceTicket<T>> getTargets() {
        return targets;
    }
    /**
     * 
     * @return 
     */
    public int getExportGroupId() {
        return exportGroupId;
    }
    
    @Override
    public String toString() {
        return "Ticket[name=" + name + ", worldIndex=" + getWorldIndex() + "]";
    }
    
    
    /**
     * Returns true if the ticket is valid for locating a resource.
     * <p>
     * A ticket is only valid if it is not null and its {@link #getWorldIndex() worldIndex}
     * is greater than or equal to zero.
     * 
     * @param ticket
     * @return true if ticket is valid
     */
    public static boolean validate(ResourceTicket ticket) {
        return ticket != null && ticket.getWorldIndex() >= 0;
    }
    
    /**
     * 
     * @param name 
     */
    public static void validateUserTicketName(String name) {
        if (name.startsWith(RESERVED)) {
            throw new IllegalArgumentException("Cannot start ticket name with reserved \""+RESERVED+"\".");
        }
    }
    
}
