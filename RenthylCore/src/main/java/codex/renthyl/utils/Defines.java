/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.utils;

import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.VarType;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 *
 * @author codex
 */
public class Defines {
    
    private static final HashMap<String, Defines> handlers = new HashMap<>();
    
    public static void config(MaterialDef matdef, String technique, Consumer<UnmappedDefines> config) {
        String name = matdef.getAssetName() + ":" + technique;
        Defines defs = handlers.get(name);
        if (defs == null) {
            defs = new Defines(matdef);
            handlers.put(name, defs);
        }
        defs.config(technique, config);
    }
    public static UnmappedDefines get(MaterialDef matdef, TechniqueDef technique) {
        String name = matdef.getAssetName() + ":" + technique.getName();
        Defines handler = handlers.get(name);
        if (handler == null) {
            throw new NullPointerException(name + " is not configured.");
        }
        return handler.get(technique);
    }
    
    private final MaterialDef matdef;
    private final LinkedList<UnmappedDefines> defines = new LinkedList<>();

    public Defines(MaterialDef matdef) {
        this.matdef = matdef;
    }
    
    public void config(String techniqueName, Consumer<UnmappedDefines> config) {
        if (!defines.isEmpty()) {
            for (UnmappedDefines d : defines) {
                config.accept(d);
            }
        } else for (TechniqueDef t : matdef.getTechniqueDefs(techniqueName)) {
            UnmappedDefines defs = new UnmappedDefines(t);
            config.accept(defs);
            defines.add(defs);
        }
    }
    public UnmappedDefines get(TechniqueDef technique) {
        for (UnmappedDefines d : defines) {
            if (d.technique == technique) {
                return d;
            }
        }
        throw new NullPointerException("No unmapped defines found for " + technique);
    }
    
    public static class UnmappedDefines {
        
        private final TechniqueDef technique;
        private final HashMap<String, Integer> defines = new HashMap<>();

        private UnmappedDefines(TechniqueDef technique) {
            this.technique = technique;
        }
        
        public UnmappedDefines add(String name, VarType type) {
            if (!defines.containsKey(name)) {
                defines.put(name, technique.addShaderUnmappedDefine(name, type));
            }
            return this;
        }
        
        public int get(String name) {
            return defines.get(name);
        }
        
    }
            
}
