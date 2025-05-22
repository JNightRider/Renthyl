/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.definitions.TextureDef;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author gary
 */
public class ShadowMapDef implements ResourceDef<ShadowMap> {

    private final TextureDef<Texture2D> mapDef = TextureDef.texture2D();
    
    public ShadowMapDef() {
        mapDef.setFormat(Image.Format.Depth);
    }
    
    @Override
    public ShadowMap createResource() {
        return new ShadowMap(mapDef.createResource());
    }

    @Override
    public Float evaluateResource(Object resource) {
        if (resource instanceof ShadowMap shadow) {
            return mapDef.evaluateResource(shadow.getMap());
        } else if (resource instanceof Texture) {
            return mapDef.evaluateResource(resource);
        }
        return null;
    }

    @Override
    public ShadowMap conformResource(Object resource) {
        if (resource instanceof ShadowMap shadow) {
            Texture2D tex = mapDef.conformResource(shadow.getMap());
            if (tex != shadow.getMap()) {
                return new ShadowMap(tex);
            } else {
                return shadow;
            }
        } else if (resource instanceof Texture) {
            return new ShadowMap(mapDef.conformResource(resource));
        }
        return null;
    }

    @Override
    public void dispose(ShadowMap object) {
        object.getMap().getImage().dispose();
    }
    
    public TextureDef<Texture2D> getMapDef() {
        return mapDef;
    }

}
