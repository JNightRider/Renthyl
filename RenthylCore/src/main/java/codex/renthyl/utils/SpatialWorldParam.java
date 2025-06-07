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
package codex.renthyl.utils;

import com.jme3.scene.Spatial;
import java.util.function.Function;

/**
 * Tracks the world value of a spatial parameter by userdata.
 * 
 * @author codex
 * @param <T>
 */
public abstract class SpatialWorldParam <T> {
    
    protected final String userdata;
    protected final T start;
    protected final T inherit;
    
    /**
     * 
     * @param userdata
     * @param start
     * @param inherit 
     */
    public SpatialWorldParam(String userdata, T start, T inherit) {
        this.userdata = userdata;
        this.start = start;
        this.inherit = inherit;
    }
    
    /**
     * Gets the local parameter value of the spatial.
     * 
     * @param spatial
     * @return 
     */
    protected abstract T getLocalValue(Spatial spatial);
    
    /**
     * Writes the calculated parameter value to the spatial.
     * 
     * @param spatial
     * @param value 
     */
    protected abstract void saveWorldValue(Spatial spatial, T value);
    
    /**
     * Gets the saved world parameter value from the spatial.
     * 
     * @param spatial
     * @return 
     */
    public abstract T getWorldValue(Spatial spatial);
    
    /**
     * Calculates and saves the world parameter value of the spatial
     * according to the world value of the spatial's parent.
     * 
     * @param spatial 
     */
    public void apply(Spatial spatial) {
        T value = getLocalValue(spatial);
        if (spatial.getParent() != null) {
            T parentVal = getWorldValue(spatial.getParent());
            if (parentVal != null && isInherit(value)) {
                saveWorldValue(spatial, parentVal);
                return;
            }
        }
        if (!isInherit(value)) {
            saveWorldValue(spatial, value);
        } else {
            saveWorldValue(spatial, start);
        }
    }
    
    /**
     * Returns true if the parameter value indicates inheritance.
     * 
     * @param value
     * @return 
     */
    public boolean isInherit(T value) {
        return value == null || value.equals(inherit);
    }
    
    /**
     * Gets the userdata key used to store a local parameter in userdata.
     * 
     * @return userdata key, or null if userdata storage is not supported
     */
    public String getUserDataKey() {
        return userdata;
    }
    
    /**
     * Gets the starting value used if a spatial's parameter
     * value cannot be determined.
     * 
     * @return 
     */
    public T getStart() {
        return start;
    }
    
    /**
     * Gets the value indicating that a spatial should
     * inherit the value from its parent.
     * 
     * @return 
     */
    public T getInherit() {
        return inherit;
    }
    
    public static <T> T getWorldParameter(Spatial subject, T inherit, T defState, Function<Spatial, T> extract) {
        while (subject != null) {
            T val = extract.apply(subject);
            if (val != null && !val.equals(inherit)) {
                return val;
            }
            subject = subject.getParent();
        }
        return defState;
    }
    
    /**
     * Calculates the world render queue parameter.
     */
    public static final SpatialWorldParam<String> RenderQueueParam = new SpatialWorldParam<>("RenderQueue", "Opaque", "Inherit") {
        
        private static final String RESULT = "ResultRenderQueue";

        @Override
        protected String getLocalValue(Spatial spatial) {
            String value = spatial.getUserData(userdata);
            if (value == null) {
                value = spatial.getLocalQueueBucket().name();
            }
            return value;
        }
        @Override
        protected void saveWorldValue(Spatial spatial, String value) {
            spatial.setUserData(RESULT, value);
        }
        @Override
        public String getWorldValue(Spatial spatial) {
            return spatial.getUserData(RESULT);
        }
        
    };
    
    
}
