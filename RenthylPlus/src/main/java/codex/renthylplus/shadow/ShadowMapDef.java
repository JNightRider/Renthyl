/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.definitions.AbstractResourceDef;
import codex.renthyl.definitions.TextureDef;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author gary
 */
public class ShadowMapDef extends AbstractResourceDef<ShadowMap> {

    private TextureDef<Texture2D> mapDef = TextureDef.texture2D();
    
    public ShadowMapDef() {
        mapDef.setFormat(Image.Format.Depth);
        mapDef.setAllowCasualAllocation(false);
        //mapDef.setShadowCompare(Texture.ShadowCompareMode.LessOrEqual);
        //mapDef.setMagFilter(Texture.MagFilter.Nearest);
        //mapDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
    }
    
    @Override
    public ShadowMap createResource() {
        return new ShadowMap(mapDef.createResource());
    }
    @Override
    public float evaluateResource(Object resource) {
        if (resource instanceof ShadowMap) {
            ShadowMap shadow = (ShadowMap)resource;
            return mapDef.evaluateResource(shadow.getMap());
        } else if (resource instanceof Texture) {
            return mapDef.evaluateResource(resource);
        }
        return Float.POSITIVE_INFINITY;
    }
    @Override
    public ShadowMap applyResource(Object resource) {
        if (resource instanceof ShadowMap) {
            ShadowMap shadow = (ShadowMap)resource;
            Texture2D tex = mapDef.applyResource(shadow.getMap());
            if (tex != shadow.getMap()) {
                return new ShadowMap(tex);
            } else {
                return shadow;
            }
        } else if (resource instanceof Texture) {
            return new ShadowMap(mapDef.applyResource(resource));
        }
        return null;
    }
    
    public TextureDef<Texture2D> getMapDef() {
        return mapDef;
    }
    
}
