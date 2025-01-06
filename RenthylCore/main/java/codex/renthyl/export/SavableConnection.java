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
package codex.renthyl.export;

import codex.renthyl.modules.RenderModule;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.Objects;

/**
 * Represents a connection of tickets between render passes.
 * 
 * @author codex
 */
public abstract class SavableConnection implements Savable {
    
    private int sourceModuleId = -1;
    private int targetModuleId = -1;
    private String sourceGroup, targetGroup;
    
    public SavableConnection() {}
    
    public void setSourceModuleId(int sourceModuleId) {
        this.sourceModuleId = sourceModuleId;
    }
    public void setTargetModuleId(int targetModuleId) {
        this.targetModuleId = targetModuleId;
    }
    public void setSourceGroup(String sourceGroup) {
        this.sourceGroup = sourceGroup;
    }
    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }
    
    public int getSourceModuleId() {
        return sourceModuleId;
    }
    public int getTargetModuleId() {
        return targetModuleId;
    }
    public String getSourceGroup() {
        return sourceGroup;
    }
    public String getTargetGroup() {
        return targetGroup;
    }
    
    public <T extends SavableConnection> T getAs(Class<T> type) {
        if (!type.isAssignableFrom(getClass())) {
            throw new ClassCastException("Expected connection as " + type.getName()
                    + " but received " + getClass().getName());
        }
        return (T)this;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        if (sourceModuleId < 0) {
            throw new IOException("Source module not specified.");
        }
        if (targetModuleId < 0) {
            throw new IOException("Target module not specified.");
        }
        Objects.requireNonNull(sourceGroup, "Source group not specified.");
        Objects.requireNonNull(targetGroup, "Target group not specified.");
        out.write(sourceModuleId, "sourceModuleId", 0);
        out.write(targetModuleId, "targetModuleId", 0);
        out.write(sourceGroup, "sourceGroup", RenderModule.MAIN_GROUP);
        out.write(targetGroup, "targetGroup", RenderModule.MAIN_GROUP);
        write(out);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        sourceModuleId = in.readInt("sourceModuleId", 0);
        targetModuleId = in.readInt("targetModuleId", 0);
        sourceGroup = in.readString("sourceGroup", RenderModule.MAIN_GROUP);
        targetGroup = in.readString("targetGroup", RenderModule.MAIN_GROUP);
        read(in);
    }
    
    protected abstract void write(OutputCapsule out) throws IOException;
    protected abstract void read(InputCapsule in) throws IOException;
    
}
