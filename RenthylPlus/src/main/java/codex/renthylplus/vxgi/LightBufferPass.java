/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.definitions.arrays.FloatArrayDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.tasks.AbstractTask;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author codex
 */
public class LightBufferPass extends AbstractTask {

    public static final int FLOATS_PER_LIGHT = 12;

    private final CollectorSocket<Light> lights = new CollectorSocket<>(this);
    private final TransitiveSocket<Light[]> lightShadowMapping = new TransitiveSocket<>(this);
    private final AllocationSocket<float[]> lightBuffer;
    private final ValueSocket<ColorRGBA> ambient = new ValueSocket<>(this, new ColorRGBA());
    private final ValueSocket<Collection<LightProbe>> probe = new ValueSocket<>(this, new ArrayList<>());
    private final FloatArrayDef arrayDef = new FloatArrayDef();
    private final ColorRGBA tempColor = new ColorRGBA(0, 0, 0, 0);

    public LightBufferPass(ResourceAllocator allocator) {
        addSockets(lights, lightShadowMapping, ambient, probe);
        lightBuffer = addSocket(new AllocationSocket<>(this, allocator, arrayDef));
        arrayDef.setPadding(0);
    }

    @Override
    protected void renderTask() {
        ambient.getValue().set(0, 0, 0, 0);
        probe.getValue().clear();
        Collection<Light> list = lights.acquire();
        arrayDef.setSize(list.size() * FLOATS_PER_LIGHT);
        Light[] shadows = lightShadowMapping.acquire();
        float[] array = lightBuffer.acquire();
        int i = 0;
        for (Light l : list) {
            if (l.getType() == Light.Type.Ambient) {
                ambient.getValue().addLocal(l.getColor());
                continue;
            }
            if (l.getType() == Light.Type.Probe) {
                probe.getValue().add((LightProbe)l);
                continue;
            }
            int id = l.getType().getId();
            if (shadows != null) {
                if (id > 3) {
                    throw new IllegalStateException("Light type id is larger than two bits: cannot pack shadow indices.");
                }
                int shadowIndex = indexOf(shadows, l);
                if (shadowIndex >= 0) {
                    id += (shadowIndex + 1) << 2;
                }
            }
            // todo: utilize unused elements
            tempColor.set(l.getColor()).setAlpha(id);
            packColor(array, i, tempColor);
            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight)l;
                    packVector(array, i + 4, dl.getDirection());
                    break;
                case Point:
                    PointLight pl = (PointLight)l;
                    packVector(array, i + 4, pl.getPosition());
                    array[i + 7] = pl.getInvRadius();
                    break;
                case Spot:
                    SpotLight sl = (SpotLight)l;
                    packVector(array, i + 4, sl.getPosition());
                    array[i + 7] = sl.getInvSpotRange();
                    packVector(array, i + 8, sl.getDirection());
                    array[i + 11] = sl.getPackedAngleCos();
                    break;
            }
            i += FLOATS_PER_LIGHT;
        }
    }
    
    private int packColor(float[] array, int i, ColorRGBA color) {
        array[i++] = color.r;
        array[i++] = color.g;
        array[i++] = color.b;
        array[i++] = color.a;
        return i;
    }
    private int packVector(float[] array, int i, Vector3f vec) {
        array[i++] = vec.x;
        array[i++] = vec.y;
        array[i++] = vec.z;
        return i;
    }
    private int indexOf(Object[] array, Object element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == element) {
                return i;
            }
            if (array[i] == null) {
                return -1;
            }
        }
        return -1;
    }

    public CollectorSocket<Light> getLights() {
        return lights;
    }

    public PointerSocket<Light[]> getLightShadowMapping() {
        return lightShadowMapping;
    }

    public Socket<float[]> getLightBuffer() {
        return lightBuffer;
    }

    public Socket<ColorRGBA> getAmbient() {
        return ambient;
    }

    public Socket<Collection<LightProbe>> getProbe() {
        return probe;
    }

}
