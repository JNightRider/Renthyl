/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceException;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author gary
 */
public class ShadowMapDef extends AbstractResourceDef<ShadowMap> {

    private final TextureDef<Texture2D> mapDef = TextureDef.texture2D();
    
    public ShadowMapDef() {
        mapDef.setFormat(Image.Format.Depth);
        //mapDef.setShadowCompare(Texture.ShadowCompareMode.LessOrEqual);
        //mapDef.setMagFilter(Texture.MagFilter.Nearest);
        //mapDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
    }
    
    @Override
    public ShadowMap createResource() {
        return new ShadowMap(mapDef.createResource());
    }
    @Override
    public Float evaluateResource(Object resource) {
        if (resource instanceof ShadowMap) {
            ShadowMap shadow = (ShadowMap)resource;
            return mapDef.evaluateResource(shadow.getMap());
        } else if (resource instanceof Texture) {
            return mapDef.evaluateResource(resource);
        }
        return null;
    }
    @Override
    public ShadowMap conformResource(Object resource) throws ResourceException {
        if (resource instanceof ShadowMap) {
            ShadowMap shadow = (ShadowMap)resource;
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
    
    public TextureDef<Texture2D> getMapDef() {
        return mapDef;
    }
    
}
