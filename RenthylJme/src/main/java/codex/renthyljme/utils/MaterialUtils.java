package codex.renthyljme.utils;

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MaterialUtils {

    public static final String JME_PBR_LIGHTING = "Common/MatDefs/Light/PBRLighting.j3md";
    public static final String RENTHYL_PBR_LIGHTING = "RenthylJme/MatDefs/PBRLighting.j3md";

    /**
     * Migrates all materials in {@code scene} by creating a new material and transfering all
     * applicable parameters from the old material to the new. Specific material usage across
     * geometries is conserved.
     *
     * @param scene scene to upgrade materials of (not null)
     * @param filter selects the materials to upgrade (not null)
     * @param factory creates the upgraded materials (not null)
     */
    public static void migrateMaterials(Spatial scene, Predicate<Material> filter, Supplier<Material> factory) {
        record MaterialMap(Material original, Material upgrade) {}
        Collection<MaterialMap> mappings = new ArrayList<>();
        graph: for (Spatial s : new SceneGraphIterator(scene)) {
            if (s instanceof Geometry) {
                Material m = ((Geometry)s).getMaterial();
                if (!filter.test(m)) {
                    continue;
                }
                for (MaterialMap map : mappings) {
                    if (map.original() == m) {
                        s.setMaterial(map.upgrade());
                        continue graph;
                    }
                }
                MaterialMap map = new MaterialMap(m, factory.get());
                for (MatParam p : m.getParams()) {
                    if (map.upgrade().getMaterialDef().getMaterialParam(p.getName()) != null) {
                        map.upgrade().setParam(p.getName(), p.getVarType(), p.getValue());
                    }
                }
                s.setMaterial(map.upgrade());
                mappings.add(map);
            }
        }
    }

    /**
     * {@link #migrateMaterials(Spatial, Predicate, Supplier) Migrates} from
     * {@link #JME_PBR_LIGHTING} to {@link #RENTHYL_PBR_LIGHTING}.
     *
     * @param assetManager
     * @param scene
     */
    public static void migratePBRLighting(AssetManager assetManager, Spatial scene) {
        migrateMaterials(scene,
                m -> m.getMaterialDef().getAssetName().equals(JME_PBR_LIGHTING),
                () -> new Material(assetManager, RENTHYL_PBR_LIGHTING));
    }

    /**
     * Returns true if {@code parameter} exists as a parameter in the material.
     *
     * @param material
     * @param parameter
     * @return
     */
    public static boolean parameterExists(Material material, String parameter) {
        return material.getMaterialDef().getMaterialParam(parameter) != null;
    }

}
