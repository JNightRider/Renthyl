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
package codex.renthyl.definitions;

import codex.renthyl.resources.EvaluatedResource;
import codex.renthyl.resources.RenderObject;

import java.util.function.Consumer;

/**
 * Manages the behavior of a {@link codex.renthyl.resources.ResourceView}, especially for creation,
 * reallocation, and disposal of related raw resources.
 * 
 * @author codex
 * @param <T>
 */
public interface ResourceDef <T> {
    
    /**
     * Creates a new resources from scratch.
     * 
     * @return 
     */
    T createResource();
    
    /**
     * Checks if the resource can be allocated.
     * 
     * @param resource
     * @return the resource if approved, otherwise null
     */
    float evaluateResource(Object resource);

    /**
     * Configures the resource for allocation once it has been chosen.
     *
     * @param resource
     */
    T applyResource(Object resource);
    
    /**
     * Returns the number of frames which the resource must be
     * static (unused throughout rendering) before it is disposed.
     * <p>
     * If negative, the default timeout value will be used instead.
     * 
     * @return static timeout duration
     */
    default int getStaticTimeout() {
        return -1;
    }
    
    /**
     * Gets the Consumer used to dispose of a resource.
     * 
     * @return resource disposer, or null
     */
    default Consumer<T> getDisposalMethod() {
        return null;
    }
    
    /**
     * Returns true if resources can be reallocated to this definition.
     * 
     * @return 
     */
    default boolean isUseExisting() {
        return true;
    }
    
    /**
     * Returns true if reallocation of this definition's resource is allowed
     * casually without a specific object id.
     * 
     * @return 
     */
    default boolean isAllowCasualAllocation() {
        return true;
    }
    
    /**
     * Returns true if reserving this definition's resource is allowed.
     * 
     * @return 
     */
    default boolean isAllowReservations() {
        return true;
    }
    
    /**
     * Returns true if the resource should be disposed after being
     * released and having no users.
     * 
     * @return 
     */
    default boolean isDisposeOnRelease() {
        return false;
    }
    
    /**
     * Returns true if the resource can be read concurrently.
     * 
     * @return 
     */
    default boolean isReadConcurrent() {
        return true;
    }
    
    /**
     * Disposes the resource using the disposal method, if not null.
     * 
     * @param resource 
     */
    default void dispose(T resource) {
        Consumer<T> d = getDisposalMethod();
        if (d != null) d.accept(resource);
    }
    
}
